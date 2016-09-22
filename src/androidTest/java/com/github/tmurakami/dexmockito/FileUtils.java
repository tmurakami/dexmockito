package com.github.tmurakami.dexmockito;

import java.io.File;

final class FileUtils {

    private FileUtils() {
        throw new AssertionError("Do not instantiate");
    }

    static boolean forceDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!forceDelete(f)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

}
