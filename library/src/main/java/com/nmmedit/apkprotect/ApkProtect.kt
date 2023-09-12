package com.nmmedit.apkprotect

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import com.mcal.apkparser.xml.ManifestParser
import com.mcal.apkparser.zip.ZipEntry
import com.mcal.apkparser.zip.ZipFile
import com.mcal.apkparser.zip.ZipOutputStream
import com.nmmedit.apkprotect.data.Prefs
import com.nmmedit.apkprotect.data.Storage
import com.nmmedit.apkprotect.dex2c.Dex2c
import com.nmmedit.apkprotect.dex2c.GlobalDexConfig
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter
import com.nmmedit.apkprotect.dex2c.converter.structs.RegisterNativesUtilClassDef
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter
import com.nmmedit.apkprotect.log.ApkLogger
import com.nmmedit.apkprotect.sign.ApkVerifyCodeGenerator
import com.nmmedit.apkprotect.util.FileHelper
import com.nmmedit.apkprotect.util.ZipHelper
import org.jetbrains.annotations.Contract
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

class ApkProtect private constructor(
    private val apkFolders: ApkFolders,
    private val instructionRewriter: InstructionRewriter,
    apkVerifyCodeGenerator: ApkVerifyCodeGenerator?,
    filter: ClassAndMethodFilter?,
    classAnalyzer: ClassAnalyzer?,
    apkLogger: ApkLogger?
) {
    private val apkVerifyCodeGenerator: ApkVerifyCodeGenerator?
    private val filter: ClassAndMethodFilter?
    private val classAnalyzer: ClassAnalyzer?
    private val apkLogger: ApkLogger?

    init {
        this.apkVerifyCodeGenerator = apkVerifyCodeGenerator
        this.filter = filter
        this.classAnalyzer = classAnalyzer
        this.apkLogger = apkLogger
    }

    @Throws(IOException::class)
    fun run() {
        val apkFile = apkFolders.inApk
        val zipExtractDir = Storage.zipExtractTempDir
        try {
            val manifestBytes = ZipHelper.getZipFileContent(apkFile, ANDROID_MANIFEST_XML)
            val parser = ManifestParser(manifestBytes)
            val packageName = parser.getPackageName()

            //生成一些需要改变的c代码(随机opcode后的头文件及apk验证代码等)
            if (!packageName.isNullOrEmpty()) {
                generateCSources(packageName)
            }

            //解压得到所有classesN.dex
            val files = getClassesFiles(apkFile, zipExtractDir)
            if (files.isEmpty()) {
                throw RuntimeException("No classes.dex")
            }
            val minSdk = parser.getMinSdkVersion()
            if (!minSdk.isNullOrEmpty()) {
                try {
                    classAnalyzer?.setMinSdk(minSdk.toInt())
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }

            //先加载apk包含的所有dex文件,以便分析一些有问题的代码
            for (file in files) {
                classAnalyzer?.loadDexFile(file)
            }

            if (filter == null) {
                throw RuntimeException("filter == null")
            }
            if (classAnalyzer == null) {
                throw RuntimeException("classAnalyzer == null")
            }

            //globalConfig里面configs顺序和classesN.dex文件列表一样
            val globalConfig: GlobalDexConfig = Dex2c.handleAllDex(
                files,
                filter,
                instructionRewriter,
                classAnalyzer,
                Storage.codeGeneratedDir
            )

            //需要放在主dex里的类
            val mainDexClassTypeSet: Set<String> = HashSet()
            //todo 可能需要通过外部配置来保留主dex需要的class

            //在处理过的class的静态初始化方法里插入调用注册本地方法的指令
            //static {
            //    NativeUtils.initClass(0);
            //}
            val outDexFiles = injectInstructionAndWriteToFile(
                globalConfig,
                mainDexClassTypeSet,
                60000,
                Storage.tempDexDir
            )
            val abis = getAbis(apkFile)
            val nativeLibs = generateNativeLibs(abis)
            val mainDex = outDexFiles[0]
            val newManDex = internNativeUtilClassDef(
                mainDex,
                globalConfig,
                Prefs.getNmmpName()
            )
            //替换为新的dex
            outDexFiles[0] = newManDex
            val outputApk = apkFolders.outputApk
            if (outputApk.exists()) {
                outputApk.delete()
            }

            ZipFile(apkFile).use { zipFile ->
                ZipOutputStream(apkFolders.outputApk).use { zos ->
                    //add AndroidManifest.xml
                    apkLogger?.info("Add AndroidManifest.xml")
                    zos.apply {
                        putNextEntry("AndroidManifest.xml")
                        write(manifestBytes)
                    }.closeEntry()
                    //add classesX.dex
                    var name: String
                    for (file in outDexFiles) {
                        name = file.name
                        apkLogger?.info("Add $name")
                        zos.apply {
                            putNextEntry(name)
                            write(file.readBytes())
                        }.closeEntry()
                    }
                    //add native libs
                    for ((abi, value) in nativeLibs) {
                        for (file in value) {
                            //最小sdk如果不小于23,且AndroidManifest.xml里面没有android:extractNativeLibs="true", so不能压缩,且需要页对齐
//                        final Source source = Sources.from(file, "lib/" + abi + "/" + file.getName(), Deflater.NO_COMPRESSION);
//                        source.align(4*1024);
                            //todo 增加处理不需要压缩的.so文件
                            name = file.name
                            apkLogger?.info("Add $name")
                            zos.apply {
                                putNextEntry("lib/$abi/$name")
                                write(file.readBytes())
                            }.closeEntry()
                        }
                    }
                    //添加原apk不被修改的数据
                    apkLogger?.info("Build apk")
                    val enumeration = zipFile.entries
                    while (enumeration.hasMoreElements()) {
                        val ze = enumeration.nextElement()
                        val regex = Pattern.compile(
                            "classes(\\d)*\\.dex" +
//                                    "|META-INF/.*\\.(RSA|DSA|EC|SF|MF)" +
                                    "|AndroidManifest\\.xml"
                        )
                        if (regex.matcher(ze.name).matches()) {
                            continue
                        }
                        zos.copyZipEntry(ze, zipFile)
                    }
                }
            }
        } finally {
            //删除解压缓存目录
            FileHelper.deleteFile(zipExtractDir)
        }
    }

    @Throws(IOException::class)
    fun generateNativeLibs(abis: List<String>): Map<String, List<File>> {
        val outRootDir = Storage.outRootDir
        val allLibs: MutableMap<String, List<File>> = HashMap()
        for (abi in abis) {
            val cmakeOptions = BuildNativeLib.CMakeOptions(
                21,
                outRootDir.absolutePath,
                BuildNativeLib.CMakeOptions.BuildType.RELEASE,
                abi
            )

            //删除上次创建的目录
            FileHelper.deleteFile(File(cmakeOptions.getBuildPath()))
            val files = BuildNativeLib(apkLogger).build(cmakeOptions)
            allLibs[abi] = files
        }
        return allLibs
    }

    @Throws(IOException::class)
    private fun generateCSources(packageName: String) {
        val vmsrcFile = File(Storage.binDir, "vmsrc.zip")
        //每次强制从资源里复制出来
        //copy vmsrc.zip to external directory
        val inputStream = ApkProtect::class.java.getResourceAsStream("/vmsrc.zip")
        if (inputStream != null) {
            FileHelper.writeToFile(vmsrcFile, inputStream)
        }
        val cSources: List<File> = ZipHelper.extractFiles(vmsrcFile, ".*", Storage.dex2cSrcDir)

        //处理指令及apk验证,生成新的c文件
        for (source in cSources) {
            if (source.name.endsWith("DexOpcodes.h")) {
                //根据指令重写规则重新生成DexOpcodes.h文件
                writeOpcodeHeaderFile(source, instructionRewriter)
            } else if (source.name.endsWith("apk_verifier.c")) {
                //根据公钥数据生成签名验证代码
                writeApkVerifierFile(packageName, source, apkVerifyCodeGenerator)
            } else if (source.name == "CMakeLists.txt") {
                //处理cmake里配置的本地库名
                writeCmakeFile(source, Prefs.getNmmpName(), Prefs.getVmName())
            }
        }
    }

    class Builder(
        private val apkFolders: ApkFolders
    ) {
        private var instructionRewriter: InstructionRewriter? = null
        private var apkVerifyCodeGenerator: ApkVerifyCodeGenerator? = null
        private var filter: ClassAndMethodFilter? = null
        private var apkLogger: ApkLogger? = null
        private var classAnalyzer: ClassAnalyzer? = null

        fun setInstructionRewriter(instructionRewriter: InstructionRewriter?): Builder {
            this.instructionRewriter = instructionRewriter
            return this
        }

        fun setApkVerifyCodeGenerator(apkVerifyCodeGenerator: ApkVerifyCodeGenerator?): Builder {
            this.apkVerifyCodeGenerator = apkVerifyCodeGenerator
            return this
        }

        fun setFilter(filter: ClassAndMethodFilter?): Builder {
            this.filter = filter
            return this
        }

        fun setLogger(apkLogger: ApkLogger?): Builder {
            this.apkLogger = apkLogger
            return this
        }

        fun setClassAnalyzer(classAnalyzer: ClassAnalyzer?): Builder {
            this.classAnalyzer = classAnalyzer
            return this
        }

        fun build(): ApkProtect {
            if (instructionRewriter == null) {
                throw RuntimeException("instructionRewriter == null")
            }
            if (classAnalyzer == null) {
                throw RuntimeException("classAnalyzer==null")
            }
            return ApkProtect(
                apkFolders,
                instructionRewriter!!,
                apkVerifyCodeGenerator,
                filter,
                classAnalyzer,
                apkLogger
            )
        }
    }

    companion object {
        const val ANDROID_MANIFEST_XML = "AndroidManifest.xml"
        const val ANDROID_APP_APPLICATION = "android.app.Application"

        //根据apk里文件得到abi，如果没有本地库则返回所有
        @Throws(IOException::class)
        private fun getAbis(apk: File): List<String> {
            val pattern = Pattern.compile("lib/(.*)/.*\\.so")
            val zipFile = ZipFile(apk)
            val entries: Enumeration<out ZipEntry> = zipFile.entries
            val abis: MutableSet<String> = HashSet()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry = entries.nextElement()
                val matcher = pattern.matcher(entry.name)
                if (matcher.matches()) {
                    abis.add(matcher.group(1))
                }
            }
            //不支持armeabi，可能还要删除mips相关
            abis.remove("armeabi")
            abis.remove("mips")
            abis.remove("mips64")
            if (abis.isEmpty()) {
                //默认只生成armeabi-v7a
                val abi = ArrayList<String>()
                if (Prefs.isArm()) {
                    abi.add("armeabi-v7a")
                }
                if (Prefs.isArm64()) {
                    abi.add("arm64-v8a")
                }
                if (Prefs.isX86()) {
                    abi.add("x86")
                }
                if (Prefs.isX64()) {
                    abi.add("x86_64")
                }
                return abi
            }
            return ArrayList(abis)
        }

        @Throws(IOException::class)
        private fun getClassesFiles(apkFile: File, zipExtractDir: File): List<File> {
            val files: List<File> = ZipHelper.extractFiles(apkFile, "classes(\\d+)*\\.dex", zipExtractDir)
            // Сортировка файлов по индексу классов в их именах
            files.sortedWith { file1, file2 ->
                val numb1 = file1.name.replace("classes", "").replace(".dex", "").toIntOrNull() ?: 0
                val numb2 = file2.name.replace("classes", "").replace(".dex", "").toIntOrNull() ?: 0
                numb1 - numb2
            }
            return files
        }

        //根据指令重写规则,重新生成新的opcode
        @Throws(IOException::class)
        fun writeOpcodeHeaderFile(source: File, instructionRewriter: InstructionRewriter) {
            val bufferedReader = BufferedReader(
                InputStreamReader(
                    FileInputStream(source), StandardCharsets.UTF_8
                )
            )
            val collect: String = bufferedReader.lines().collect(Collectors.joining("\n"))
            val opcodePattern = Pattern.compile(
                "enum Opcode \\{.*?};",
                Pattern.MULTILINE or Pattern.DOTALL
            )
            val opcodeContent = StringWriter()
            val gotoTableContent = StringWriter()
            instructionRewriter.generateConfig(opcodeContent, gotoTableContent)
            var headerContent = opcodePattern
                .matcher(collect)
                .replaceAll(String.format("enum Opcode {\n%s};\n", opcodeContent.toString()))

            //根据opcode生成goto表
            val patternGotoTable = Pattern.compile(
                "_name\\[kNumPackedOpcodes] = \\{.*?};",
                Pattern.MULTILINE or Pattern.DOTALL
            )
            headerContent = patternGotoTable
                .matcher(headerContent)
                .replaceAll(String.format("_name[kNumPackedOpcodes] = {        \\\\\n%s};\n", gotoTableContent))
            FileWriter(source).use { fileWriter -> fileWriter.write(headerContent) }
        }

        //读取证书信息,并把公钥写入签名验证文件里,运行时对apk进行签名校验
        @Throws(IOException::class)
        private fun writeApkVerifierFile(
            packageName: String,
            source: File,
            apkVerifyCodeGenerator: ApkVerifyCodeGenerator?
        ) {
            if (apkVerifyCodeGenerator == null) {
                return
            }
            val bufferedReader = BufferedReader(
                InputStreamReader(
                    FileInputStream(source), StandardCharsets.UTF_8
                )
            )
            val lines = bufferedReader.lines().collect(Collectors.joining("\n"))
            val dataPlaceHolder = "#define publicKeyPlaceHolder"
            var content = lines.replace(dataPlaceHolder.toRegex(), dataPlaceHolder + apkVerifyCodeGenerator.generate())
            content = content.replace("(#define PACKAGE_NAME) .*\n".toRegex(), "$1 \"$packageName\"\n")
            FileWriter(source).use { fileWriter -> fileWriter.write(content) }
        }

        @Throws(IOException::class)
        fun writeCmakeFile(cmakeTemp: File, libNmmpName: String, libVmName: String) {
            val bufferedReader = BufferedReader(
                InputStreamReader(
                    FileInputStream(cmakeTemp), StandardCharsets.UTF_8
                )
            )
            var lines = bufferedReader.lines().collect(Collectors.joining("\n"))
            //定位cmake里的语句,防止替换错误
            var libNameFormat = "set\\(LIBNAME_PLACEHOLDER \"%s\"\\)"

            //替换原本libname
            lines = lines.replace(
                String.format(libNameFormat, "nmmp").toRegex(), String.format(
                    libNameFormat, libNmmpName
                )
            )
            libNameFormat = "set\\(LIBNMMVM_NAME \"%s\" CACHE INTERNAL \"lib %s name\"\\)"
            lines = lines.replace(
                String.format(libNameFormat, "nmmvm", "nmmvm").toRegex(),
                String.format(libNameFormat, libVmName, libVmName)
            )
            FileWriter(cmakeTemp).use { fileWriter -> fileWriter.write(lines) }
        }

        @Throws(IOException::class)
        private fun dexWriteToFile(dexPool: DexPool, index: Int, dexOutDir: File): File {
            if (!dexOutDir.exists()) {
                dexOutDir.mkdirs()
            }
            val outDexFile = if (index == 0) {
                File(dexOutDir, "classes.dex")
            } else {
                File(dexOutDir, String.format("classes%d.dex", index + 1))
            }
            dexPool.writeTo(FileDataStore(outDexFile))
            return outDexFile
        }

        @Throws(IOException::class)
        private fun getApplicationClassesFromMainDex(
            globalConfig: GlobalDexConfig,
            applicationClass: String
        ): List<String?> {
            val mainDexClassList: MutableList<String?> = ArrayList()
            var tmpType = classDotNameToType(applicationClass)
            mainDexClassList.add(tmpType)
            for (config in globalConfig.configs) {
                val dexFile = DexBackedDexFile.fromInputStream(
                    Opcodes.getDefault(),
                    BufferedInputStream(FileInputStream(config.shellDexFile))
                )
                val classes: Set<DexBackedClassDef> = dexFile.classes
                var classDef: ClassDef?
                var superClass: String?
                while (true) {
                    classDef = getClassDefFromType(classes, tmpType)
                    if (classDef == null) {
                        break
                    } else {
                        if (classDotNameToType(ANDROID_APP_APPLICATION) == classDef.superclass) {
                            return mainDexClassList
                        }
                        superClass = classDef.superclass
                        if (superClass != null) {
                            tmpType = superClass
                        }
                        mainDexClassList.add(tmpType)
                    }
                }
            }
            return mainDexClassList
        }

        private fun getClassDefFromType(classDefSet: Set<ClassDef>, type: String): ClassDef? {
            for (classDef in classDefSet) {
                if (classDef.type == type) {
                    return classDef
                }
            }
            return null
        }

        /**
         * 给处理过的class注入静态初始化方法,同时dex适当拆分防止dex索引异常
         * 不缓存写好的dexpool,每次切换dexpool时马上把失效的dexpool写入文件,减小内存占用
         *
         * @param globalConfig
         * @param mainClassSet
         * @param maxPoolSize
         * @param dexOutDir
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun injectInstructionAndWriteToFile(
            globalConfig: GlobalDexConfig,
            mainClassSet: Set<String>,
            maxPoolSize: Int,
            dexOutDir: File
        ): ArrayList<File> {
            val dexFiles = ArrayList<File>()
            var lastDexPool = DexPool(Opcodes.getDefault())
            val configs = globalConfig.configs
            //第一个dex为main dex
            //提前处理主dex里的类
            for (config in configs) {
                val dexNativeFile = DexBackedDexFile.fromInputStream(
                    Opcodes.getDefault(),
                    BufferedInputStream(FileInputStream(config.shellDexFile))
                )
                for (classDef in dexNativeFile.classes) {
                    if (mainClassSet.contains(classDef.type)) {
                        //可能保留的类太多,导超出致dex引用,又没再接收返回的dex而导致丢失class
                        Dex2c.injectCallRegisterNativeInsns(config, lastDexPool, mainClassSet, maxPoolSize)
                    }
                }
            }
            for (i in configs.indices) {
                val config = configs[i]
                val retPools = Dex2c.injectCallRegisterNativeInsns(config, lastDexPool, mainClassSet, maxPoolSize)
                if (retPools.isEmpty()) {
                    throw RuntimeException("Dex inject instruction error")
                }
                if (retPools.size > 1) {
                    for (k in 0 until retPools.size - 1) {
                        val size = dexFiles.size
                        val file = dexWriteToFile(retPools[k], size, dexOutDir)
                        dexFiles.add(file)
                    }
                    lastDexPool = retPools[retPools.size - 1]
                    if (i == configs.size - 1) {
                        val size = dexFiles.size
                        val file = dexWriteToFile(lastDexPool, size, dexOutDir)
                        dexFiles.add(file)
                    }
                } else {
                    val size = dexFiles.size
                    val file = dexWriteToFile(retPools[0], size, dexOutDir)
                    dexFiles.add(file)
                    lastDexPool = DexPool(Opcodes.getDefault())
                }
            }
            return dexFiles
        }

        /**
         * 复制dex里所有类到新的dex里
         *
         * @param oldDexFile 原dex
         * @param newDex     目标dex
         */
        private fun copyDex(oldDexFile: DexFile, newDex: DexPool) {
            for (classDef in oldDexFile.classes) {
                newDex.internClass(classDef)
            }
        }

        //在主dex里增加NativeUtil类
        //返回处理后的dex文件
        @Throws(IOException::class)
        private fun internNativeUtilClassDef(
            mainDex: File,
            globalConfig: GlobalDexConfig,
            libName: String
        ): File {
            val mainDexFile = DexBackedDexFile.fromInputStream(
                Opcodes.getDefault(),
                BufferedInputStream(FileInputStream(mainDex))
            )
            val newDex = DexPool(Opcodes.getDefault())
            copyDex(mainDexFile, newDex)
            val nativeMethodNames = ArrayList<String>()
            for (config in globalConfig.configs) {
                nativeMethodNames.add(config.registerNativesMethodName)
            }
            newDex.internClass(
                RegisterNativesUtilClassDef(
                    "L" + globalConfig.configs[0].registerNativesClassName + ";",
                    nativeMethodNames, libName
                )
            )
            val injectLoadLib = File(mainDex.parent, "injectLoadLib")
            if (!injectLoadLib.exists()) {
                injectLoadLib.mkdirs()
            }
            val newFile = File(injectLoadLib, mainDex.name)
            newDex.writeTo(FileDataStore(newFile))
            return newFile
        }

        @Contract(pure = true)
        private fun classDotNameToType(classDotName: String): String {
            return "L" + classDotName.replace('.', '/') + ";"
        }
    }
}
