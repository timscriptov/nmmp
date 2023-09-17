package com.nmmedit.apkprotect;

import java.io.File;

public class ApkFolders {
    private final File inApk;
    private final File outApkFile;

    public ApkFolders(File inApk, File outApkFile) {
        this.inApk = inApk;
        this.outApkFile = outApkFile;
    }

    public File getInApk() {
        return inApk;
    }

    public File getOutRootDir() {
        return outApkFile.getParentFile();
    }

    //apk解压目录,生成新apk之后可以删除
    public File getZipExtractTempDir() {
        return new File(getOutRootDir(), ".apk_temp");
    }

    //c源代码输出目录
    public File getDex2cSrcDir() {
        return new File(getOutRootDir(), "dex2c");
    }

    //c文件及对应dex输出目录
    public File getCodeGeneratedDir() {
        return new File(getDex2cSrcDir(), "generated");
    }

    //在处理过的classes.dex里插入jni初始化代码及主classes.dex加载so库代码,生成新dex输出目录
    //这个目录可以删除,但是为了更好debug之类就保留了方便查看dex
    public File getTempDexDir() {
        return new File(getOutRootDir(), "dex_output");
    }

    public File getOutputApk() {
        return outApkFile;
    }
}
