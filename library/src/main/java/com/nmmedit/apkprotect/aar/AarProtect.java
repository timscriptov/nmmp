package com.nmmedit.apkprotect.aar;

import com.android.tools.r8.D8;
import com.android.tools.smali.dexlib2.iface.Method;
import com.google.common.collect.HashMultimap;
import com.mcal.apkparser.zip.ZipEntry;
import com.mcal.apkparser.zip.ZipFile;
import com.mcal.apkparser.zip.ZipOutputStream;
import com.nmmedit.apkprotect.BuildNativeLib;
import com.nmmedit.apkprotect.aar.asm.AsmMethod;
import com.nmmedit.apkprotect.aar.asm.AsmUtils;
import com.nmmedit.apkprotect.aar.asm.InjectStaticBlockVisitor;
import com.nmmedit.apkprotect.aar.asm.MethodToNativeVisitor;
import com.nmmedit.apkprotect.data.Prefs;
import com.nmmedit.apkprotect.dex2c.Dex2c;
import com.nmmedit.apkprotect.dex2c.DexConfig;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.MyMethodUtil;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.dex2c.filters.ClassAndMethodFilter;
import com.nmmedit.apkprotect.log.VmpLogger;
import com.nmmedit.apkprotect.util.CmakeUtils;
import com.nmmedit.apkprotect.util.FileHelper;
import com.nmmedit.apkprotect.util.ZipHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.util.*;

public class AarProtect {
    public static VmpLogger vmpLogger;
    private final AarFolders aarFolders;
    private final InstructionRewriter instructionRewriter;
    private final ClassAndMethodFilter filter;
    private final ClassAnalyzer classAnalyzer;

    private AarProtect(AarFolders aarFolders,
                       InstructionRewriter instructionRewriter,
                       ClassAndMethodFilter filter,
                       ClassAnalyzer classAnalyzer) {
        this.aarFolders = aarFolders;
        this.instructionRewriter = instructionRewriter;
        this.filter = filter;
        this.classAnalyzer = classAnalyzer;
    }

    private static byte[] modifyClass(InputStream inClass,
                                      @NotNull Set<List<? extends Method>> convertedMethods,
                                      String clsName,
                                      String methodName,
                                      int classIdx) throws IOException {
        Map<AsmMethod, List<AsmMethod>> myMethods = new HashMap<>();
        for (List<? extends Method> methods : convertedMethods) {
            if (methods.isEmpty()) {
                throw new IllegalStateException();
            }
            final Method method = methods.get(0);
            final String sig = MyMethodUtil.getMethodSignature(method.getParameterTypes(), method.getReturnType());
            final AsmMethod asmMethod = new AsmMethod(method.getAccessFlags(), method.getName(), sig);

            if (methods.size() == 1) {
                myMethods.put(asmMethod, Collections.singletonList(asmMethod));
            } else if (methods.size() == 2) {
                final Method method1 = methods.get(1);
                final String sig1 = MyMethodUtil.getMethodSignature(method1.getParameterTypes(), method1.getReturnType());
                final AsmMethod asmMethod1 = new AsmMethod(method1.getAccessFlags(), method1.getName(), sig1);

                myMethods.put(asmMethod, Arrays.asList(asmMethod, asmMethod1));
            }
        }
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final MethodToNativeVisitor methodToNativeVisitor = new MethodToNativeVisitor(Opcodes.ASM9, cw, myMethods);

        final InjectStaticBlockVisitor visitor = new InjectStaticBlockVisitor(Opcodes.ASM9, methodToNativeVisitor,
                clsName,
                methodName,
                classIdx);
        final ClassReader cr = new ClassReader(inClass);
        cr.accept(visitor, 0);
        return cw.toByteArray();
    }

    @Contract(pure = true)
    private static @NotNull List<String> getAbis() throws IOException {
        return Arrays.asList(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64");
    }

    private static @NotNull File getClassesDex(File dexJar, @NotNull File zipExtractDir) throws IOException {
        if (!zipExtractDir.exists()) zipExtractDir.mkdirs();
        final File file = new File(zipExtractDir, "classes.dex");

        byte[] jarBytes = ZipHelper.getZipFileContent(dexJar, "classes.dex");
        if (jarBytes == null) {
            final VmpLogger logger = vmpLogger;
            if (logger == null) {
                throw new IllegalStateException("No classes.dex");
            } else {
                logger.warning("No classes.dex");
            }
        }
        FileHelper.copyStream(FileHelper.bytesToInputStream(jarBytes), new FileOutputStream(file));
        return file;
    }

    public void run() throws IOException {
        final File aar = aarFolders.getInputAar();
        if (!aar.exists()) {
            throw new FileNotFoundException(aar.getAbsolutePath());
        }
        final File dexJar = classesJarToDexJar();

        final File zipExtractTempDir = aarFolders.apkFolders.getZipExtractTempDir();
        try {
            final File classesDex = getClassesDex(dexJar, zipExtractTempDir);

            CmakeUtils.generateCSources(aarFolders.apkFolders.getDex2cSrcDir(), instructionRewriter);


            //
            classAnalyzer.loadDexFile(classesDex);


            final DexConfig dexConfig = Dex2c.handleModuleDex(classesDex,
                    filter,
                    classAnalyzer,
                    instructionRewriter,
                    aarFolders.apkFolders.getCodeGeneratedDir());

            //根据处理过的dex信息修改转换前的class文件
            final File newClassesJar = modifyClassFiles(dexConfig);


            final Map<String, Map<File, File>> nativeLibs = BuildNativeLib.generateNativeLibs(aarFolders.apkFolders.getOutRootDir(),
                    getAbis());

            try (ZipFile zipFile = new ZipFile(aar)) {
                final File outputAarFile = aarFolders.getOutputAar();
                if (outputAarFile.exists()) FileHelper.deleteFile(outputAarFile);
                try (ZipOutputStream zos = new ZipOutputStream(outputAarFile)) {
                    final Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                    while (enumeration.hasMoreElements()) {
                        final ZipEntry ze = enumeration.nextElement();
                        final String entryName = ze.getName();
                        if ("classes.jar".equals(entryName)) {
                            continue;
                        }
                        if (ze.isDirectory()) {
                            continue;
                        }
                        zos.copyZipEntry(ze, zipFile);
                    }

                    //add classes.jar
                    zos.putNextEntry("classes.jar");
                    zos.write(FileHelper.readBytes(newClassesJar));
                    zos.closeEntry();

                    //todo 可能需要修改proguard规则文件，保留添加的新类及被修改过的class

                    //add native libs
                    for (Map.Entry<String, Map<File, File>> entry : nativeLibs.entrySet()) {
                        final String abi = entry.getKey();
                        //todo 不清楚是否应该使用未strip的.so文件
                        for (File file : entry.getValue().values()) {
                            zos.putNextEntry("jni/" + abi + "/" + file.getName());
                            zos.write(FileHelper.readBytes(file));
                            zos.closeEntry();
                        }
                    }
                }
            }
        } finally {
            FileHelper.deleteFile(dexJar);
            FileHelper.deleteFile(zipExtractTempDir);
        }
    }

    private @NotNull File modifyClassFiles(@NotNull DexConfig dexConfig) throws IOException {
        final HashMultimap<String, List<? extends Method>> modifiedMethods = dexConfig.getShellMethods();
        final File classJar = extractClassJar();
        final File outClassJar = getOutClassJar();
        if (outClassJar.exists()) FileHelper.deleteFile(outClassJar);
        try (ZipFile zipFile = new ZipFile(classJar)) {
            try (ZipOutputStream zos = new ZipOutputStream(outClassJar)) {
                final Enumeration<ZipEntry> enumeration = zipFile.getEntries();
                while (enumeration.hasMoreElements()) {
                    final ZipEntry ze = enumeration.nextElement();
                    if (ze.isDirectory()) {
                        continue;
                    }
                    final String entryName = ze.getName();

                    if (entryName.endsWith(".class")) {
                        String type = "L" + entryName.substring(0, entryName.length() - ".class".length()) + ";";
                        if (modifiedMethods.containsKey(type)) {
                            final byte[] bytes = ZipHelper.getZipFileContent(classJar, entryName);
                            if (bytes != null) {
                                final InputStream inClass = FileHelper.bytesToInputStream(bytes);
                                final Set<List<? extends Method>> methods = modifiedMethods.get(type);
                                //去除首尾L;
                                final int classIdx = dexConfig.getOffsetFromClassName(type.substring(1, type.length() - 1));
                                //生成新的class文件
                                final byte[] newClassBytes = modifyClass(inClass, methods, dexConfig.getRegisterNativesClassName(), dexConfig.getRegisterNativesMethodName(), classIdx);

                                //添加进jar中
                                zos.putNextEntry(entryName);
                                zos.write(newClassBytes);
                                zos.closeEntry();
                            }
                            continue;
                        }
                    }

                    zos.copyZipEntry(ze, zipFile);
                }

                //生成NativeUtil,添加进jar
                final byte[] bytes = AsmUtils.genCfNativeUtil(dexConfig.getRegisterNativesClassName(),
                        Prefs.getNmmpName(),
                        Collections.singletonList(dexConfig.getRegisterNativesMethodName()));
                zos.putNextEntry(dexConfig.getRegisterNativesClassName() + ".class");
                zos.write(bytes);
                zos.closeEntry();
            }
        }
        return outClassJar;
    }

    private @NotNull File extractClassJar() throws IOException {
        final File file = new File(aarFolders.getTempDir(), "classes.jar");
        if (file.exists()) {
            return file;
        }

        final byte[] jarBytes = ZipHelper.getZipFileContent(aarFolders.getInputAar(), "classes.jar");
        if (jarBytes == null) {
            final VmpLogger logger = vmpLogger;
            if (logger == null) {
                throw new IllegalStateException("No classes.jar");
            } else {
                logger.warning("No classes.jar");
            }
        }
        FileHelper.copyStream(FileHelper.bytesToInputStream(jarBytes), new FileOutputStream(file));
        return file;
    }

    @Contract(" -> new")
    private @NotNull File getOutClassJar() throws IOException {
        return new File(aarFolders.getTempDir(), "new_classes.jar");
    }

    //d8 --release --output <file> <input-files>
    //可能还需要指定--lib或者--classpath参数用于desugaring
    private @NotNull File classesJarToDexJar() throws IOException {
        final File classJar = extractClassJar();
        final File convertedDexJar = aarFolders.getConvertedDexJar();
        D8.main(new String[]{
                "--release",
//                "--lib",
//                libpath,
                "--output",
                convertedDexJar.getAbsolutePath(),
                classJar.getAbsolutePath()
        });
        return convertedDexJar;
    }

    public static class Builder {
        private final AarFolders aarFolders;
        private InstructionRewriter instructionRewriter;
        private ClassAndMethodFilter filter;
        private ClassAnalyzer classAnalyzer;


        public Builder(AarFolders aarFolders) {
            this.aarFolders = aarFolders;
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

        public void setLogger(VmpLogger logger) {
            AarProtect.vmpLogger = logger;
        }

        public AarProtect build() {
            final VmpLogger logger = vmpLogger;
            if (instructionRewriter == null) {
                if (logger == null) {
                    throw new RuntimeException("instructionRewriter == null");
                } else {
                    logger.warning("instructionRewriter == null");
                }
            }
            if (classAnalyzer == null) {
                if (logger == null) {
                    throw new RuntimeException("classAnalyzer==null");
                } else {
                    logger.warning("classAnalyzer == null");
                }
            }
            return new AarProtect(aarFolders, instructionRewriter, filter, classAnalyzer);
        }
    }
}
