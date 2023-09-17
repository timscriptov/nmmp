package com.nmmedit.apkprotect.aab;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class AabFolders {
    @NotNull
    private final File inAab;
    @NotNull
    private final File outputAabFile;

    public AabFolders(@NotNull File inAab, @NotNull File outputAabFile) {
        this.inAab = inAab;
        this.outputAabFile = outputAabFile;
    }

    @NotNull
    public File getInAab() {
        return inAab;
    }

    @NotNull
    public File getOutRootDir() {
        return outputAabFile.getParentFile();
    }

    //apk解压目录,生成新apk之后可以删除
    @NotNull
    public File getZipExtractTempDir() {
        return new File(getOutRootDir(), ".apk_temp");
    }

    //c源代码输出目录
    @NotNull
    public File getDex2cSrcDir() {
        return new File(getOutRootDir(), "dex2c");
    }

    //c文件及对应dex输出目录
    @NotNull
    public File getCodeGeneratedDir() {
        return new File(getDex2cSrcDir(), "generated");
    }

    //在处理过的classes.dex里插入jni初始化代码及主classes.dex加载so库代码,生成新dex输出目录
    //这个目录可以删除,但是为了更好debug之类就保留了方便查看dex
    @NotNull
    public File getTempDexDir() {
        return new File(getOutRootDir(), "dex_output");
    }


    @NotNull
    public File getOutputAab() {
        return outputAabFile;
    }
}
