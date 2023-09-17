package com.nmmedit.apkprotect.aab;

import com.mcal.apkparser.zip.ZipEntry;
import com.mcal.apkparser.zip.ZipFile;
import com.mcal.apkparser.zip.ZipOutputStream;
import com.nmmedit.apkprotect.ApkProtect;
import com.nmmedit.apkprotect.BuildNativeLib;
import com.nmmedit.apkprotect.aab.proto.ProtoUtils;
import com.nmmedit.apkprotect.data.Prefs;
import com.nmmedit.apkprotect.dex2c.Dex2c;
import com.nmmedit.apkprotect.dex2c.GlobalDexConfig;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import com.nmmedit.apkprotect.util.ApkUtils;
import com.nmmedit.apkprotect.util.CmakeUtils;
import com.nmmedit.apkprotect.util.FileHelper;
import com.nmmedit.apkprotect.util.ZipHelper;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AabProtect {
    public static final String ANDROID_MANIFEST_XML = "base/manifest/AndroidManifest.xml";
    public static final String BUNDLE_DEBUG_SYMBOL = "BUNDLE-METADATA/com.android.tools.build.debugsymbols/";

    public static final String BUNDLE_MAPPING = "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map";
    public static final String BUNDLE_CONFIG = "BundleConfig.pb";
    public static final String NATIVE_PB = "base/native.pb";
    @NotNull
    private final AabFolders aabFolders;
    @NotNull
    private final InstructionRewriter instructionRewriter;
    private final ClassAnalyzer classAnalyzer;
    private final ClassAndMethodFilter filter;

    public AabProtect(@NotNull AabFolders aabFolders,
                      @NotNull InstructionRewriter instructionRewriter,
                      @NotNull ClassAnalyzer classAnalyzer,
                      ClassAndMethodFilter filter) {
        this.aabFolders = aabFolders;
        this.instructionRewriter = instructionRewriter;
        this.classAnalyzer = classAnalyzer;
        this.filter = filter;
    }

    public void run() throws IOException {
        final File inAab = aabFolders.getInAab();
        final File zipExtractDir = aabFolders.getZipExtractTempDir();


        try {
            byte[] manifestBytes = ApkUtils.getFile(inAab, ANDROID_MANIFEST_XML);
            if (manifestBytes == null) {
                //错误aab文件
                throw new RuntimeException("Not is aab");
            }


            final String packageName = ProtoUtils.AndroidManifest.getPackageName(manifestBytes);


            //生成一些需要改变的c代码(随机opcode后的头文件及apk验证代码等)
            CmakeUtils.generateCSources(aabFolders.getDex2cSrcDir(), instructionRewriter);

            //解压得到所有classesN.dex
            List<File> files = getClassesFiles(inAab, zipExtractDir);
            if (files.isEmpty()) {
                throw new RuntimeException("No classes.dex");
            }
            for (File file : files) {
                classAnalyzer.loadDexFile(file);
            }
            //todo 加载系统dex用于完整分析


            //globalConfig里面configs顺序和classesN.dex文件列表一样
            final GlobalDexConfig globalConfig = Dex2c.handleAllDex(files,
                    filter,
                    instructionRewriter,
                    classAnalyzer,
                    aabFolders.getCodeGeneratedDir());


            //需要放在主dex里的类
            final Set<String> mainDexClassTypeSet = new HashSet<>();
            //todo 可能需要通过外部配置来保留主dex需要的class


            //在处理过的class的静态初始化方法里插入调用注册本地方法的指令
            //static {
            //    NativeUtils.initClass(0);
            //}
            final List<File> outDexFiles = new ArrayList<>(ApkProtect.injectInstructionAndWriteToFile(
                    globalConfig,
                    mainDexClassTypeSet,
                    60000,
                    aabFolders.getTempDexDir()));


            //主dex里处理so加载问题
            File mainDex = outDexFiles.get(0);


            final File newMainDex = ApkProtect.internNativeUtilClassDef(
                    mainDex,
                    globalConfig, Prefs.getNmmpName());
            //替换为新的dex
            outDexFiles.set(0, newMainDex);


            //编译c代码生成库,
            final List<String> abis = getAbis(inAab);
            final Map<String, Map<File, File>> nativeLibs = BuildNativeLib.generateNativeLibs(aabFolders.getOutRootDir(), abis);


            final File outputAab = aabFolders.getOutputAab();
            if (outputAab.exists()) {
                outputAab.delete();
            }

            try (ZipFile zipFile = new ZipFile(inAab)) {
                try (ZipOutputStream zos = new ZipOutputStream(outputAab)) {
                    //add new BundleConfig.pb
                    final byte[] configBytes = ApkUtils.getFile(inAab, BUNDLE_CONFIG);
                    final byte[] newConfigBytes = ProtoUtils.BundleConfig.editConfig(configBytes);

                    zos.putNextEntry(BUNDLE_CONFIG);
                    zos.write(newConfigBytes);
                    zos.closeEntry();

                    //add new AndroidManifest.xml
                    final byte[] newManifestBytes = ProtoUtils.AndroidManifest.editAndroidManifest(manifestBytes);

                    zos.putNextEntry(ANDROID_MANIFEST_XML);
                    zos.write(newManifestBytes);
                    zos.closeEntry();

                    //add classesN.dex
                    for (File file : outDexFiles) {
                        zos.putNextEntry("base/dex/" + file.getName());
                        zos.write(FileHelper.readBytes(file));
                        zos.closeEntry();
                    }

                    //add native libs
                    for (Map.Entry<String, Map<File, File>> entry : nativeLibs.entrySet()) {
                        for (Map.Entry<File, File> soEntry : entry.getValue().entrySet()) {
                            final File stripSo = soEntry.getValue();
                            //add strip so
                            zos.putNextEntry("base/lib/" + entry.getKey() + "/" + stripSo.getName());
                            zos.write(FileHelper.readBytes(stripSo));
                            zos.closeEntry();

                            //add symbol so
                            final File symSo = soEntry.getKey();
                            zos.putNextEntry(BUNDLE_DEBUG_SYMBOL + entry.getKey() + "/" + symSo.getName() + ".sym");
                            zos.write(FileHelper.readBytes(symSo));
                            zos.closeEntry();
                        }
                    }

                    //add base/native.pb
                    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ProtoUtils.NativeLibraries.writeNativePB(abis, bout);
                    zos.putNextEntry(NATIVE_PB);
                    zos.write(bout.toByteArray());
                    zos.closeEntry();

                    final Pattern regex = Pattern.compile(
                            "base/dex/classes(\\d)*\\.dex" +
                                    "|META-INF/.*\\.(RSA|DSA|EC|SF|MF)" +
                                    "|" + NATIVE_PB +
                                    "|" + ANDROID_MANIFEST_XML +
                                    "|" + BUNDLE_CONFIG);
                    final Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                    while (enumeration.hasMoreElements()) {
                        final ZipEntry ze = enumeration.nextElement();
                        final String entryName = ze.getName();
                        if (regex.matcher(entryName).matches()) {
                            continue;
                        }
                        zos.copyZipEntry(ze, zipFile);
                    }
                }
            }
        } finally {
            //删除解压缓存目录
            FileHelper.deleteFile(zipExtractDir);
        }
    }

    @NotNull
    private static List<File> getClassesFiles(File apkFile, File zipExtractDir) throws IOException {
        List<File> files = ApkUtils.extractFiles(apkFile, "base/dex/classes(\\d+)*\\.dex", zipExtractDir);
        //根据classes索引大小排序
        files.sort((file, t1) -> {
            final String numb = file.getName().replace("classes", "").replace(".dex", "");
            final String numb2 = t1.getName().replace("classes", "").replace(".dex", "");
            int n, n2;
            if ("".equals(numb)) {
                n = 0;
            } else {
                n = Integer.parseInt(numb);
            }
            if ("".equals(numb2)) {
                n2 = 0;
            } else {
                n2 = Integer.parseInt(numb2);
            }
            return n - n2;
        });
        return files;
    }

    //根据aab文件得到abi，如果没有本地库则返回x86及arm所有abi
    private static @NotNull List<String> getAbis(File aab) throws IOException {
        final Pattern pattern = Pattern.compile("base/lib/(.*)/.*\\.so");
        final Enumeration<? extends ZipEntry> entries;
        try (ZipFile zipFile = new ZipFile(aab)) {
            entries = zipFile.getEntries();

        }
        Set<String> abis = new HashSet<>();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            final Matcher matcher = pattern.matcher(entry.getName());
            if (matcher.matches()) {
                abis.add(matcher.group(1));
            }
        }
        //不支持armeabi，可能还要删除mips相关
        abis.remove("armeabi");
        abis.remove("mips");
        abis.remove("mips64");

        if (abis.isEmpty()) {
            return Arrays.asList("armeabi-v7a", "arm64-v8a", "x86", "x86_64");
        }
        return new ArrayList<>(abis);
    }


    public static @NotNull InputStream getAabProguardMapping(File aab) throws IOException {
        return FileHelper.bytesToInputStream(ZipHelper.getZipFileContent(aab, BUNDLE_MAPPING));
    }


    public static class Builder {
        private final AabFolders aabFolders;
        private InstructionRewriter instructionRewriter;
        private ClassAnalyzer classAnalyzer;
        private ClassAndMethodFilter filter;


        public Builder(AabFolders aabFolders) {
            this.aabFolders = aabFolders;
        }

        public Builder setInstructionRewriter(InstructionRewriter instructionRewriter) {
            this.instructionRewriter = instructionRewriter;
            return this;
        }


        public Builder setFilter(ClassAndMethodFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setClassAnalyzer(ClassAnalyzer classAnalyzer) {
            this.classAnalyzer = classAnalyzer;
            return this;
        }

        public AabProtect build() {
            if (instructionRewriter == null) {
                throw new RuntimeException("instructionRewriter == null");
            }
            if (classAnalyzer == null) {
                throw new RuntimeException("classAnalyzer == null");
            }
            return new AabProtect(aabFolders, instructionRewriter, classAnalyzer, filter);
        }
    }
}
