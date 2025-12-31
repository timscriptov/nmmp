package com.mcal.apkparser.zip;

public interface ZipCallback {
    void onProgress(long current, long total);
}
