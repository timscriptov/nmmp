package com.nmmedit.apkprotect.util;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class FileUtils {
    public static String getHomePath() {
        CodeSource codeSource = FileUtils.class.getProtectionDomain().getCodeSource();
        try {
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            return jarFile.getParentFile().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return getHomePath();
        }
    }
}
