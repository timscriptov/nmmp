package com.nmmedit.apkprotect.aar;

import com.nmmedit.apkprotect.ApkFolders;

import java.io.File;

public class AarFolders {
    public final ApkFolders apkFolders;
    private final File inputAarFile;
    private final File outputAarFile;

    public AarFolders(File aar, File outputAarFile) {
        this.inputAarFile = aar;
        this.outputAarFile = outputAarFile;
        this.apkFolders = new ApkFolders(getConvertedDexJar(), outputAarFile);
    }

    public File getInputAar() {
        return inputAarFile;
    }

    public File getOutputDir() {
        return outputAarFile.getParentFile();
    }


    public File getConvertedDexJar() {
        return new File(getTempDir(), getOutputDir().getName() + "-dex.jar");
    }

    public File getTempDir() {
        final File file = new File(getOutputDir(), ".dx_temp");
        if (!file.exists()) file.mkdirs();
        return file;
    }

    public File getOutputAar() {
        return outputAarFile;
    }
}
