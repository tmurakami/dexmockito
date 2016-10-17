package com.github.tmurakami.dexmockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

final class CacheDir {

    private CacheDir() {
        throw new AssertionError("Do not instantiate");
    }

    static File get(File root, ClassLoader classLoader) {
        File dataRoot = new File(root, "data/data");
        try {
            for (Enumeration<URL> e = classLoader.getResources("AndroidManifest.xml"); e.hasMoreElements(); ) {
                String path = e.nextElement().getPath();
                int hyphen = path.lastIndexOf('-');
                if (hyphen == -1) {
                    continue;
                }
                path = path.substring(0, hyphen);
                int slash = path.lastIndexOf('/');
                if (slash == -1) {
                    continue;
                }
                File data = new File(dataRoot, path.substring(slash));
                if (!data.exists()) {
                    continue;
                }
                File dir = new File(data, "cache/dexmockito");
                if (dir.isDirectory() && dir.canRead() && dir.canWrite() || dir.mkdirs()) {
                    return dir.getCanonicalFile();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new Error("Cannot access DexMockito cache directory");
    }

}
