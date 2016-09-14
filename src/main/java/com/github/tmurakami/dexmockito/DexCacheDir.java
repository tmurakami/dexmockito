package com.github.tmurakami.dexmockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

final class DexCacheDir {

    private DexCacheDir() {
        throw new AssertionError("Do not instantiate");
    }

    static File get(File root, ClassLoader classLoader) {
        Enumeration<URL> res;
        try {
            res = classLoader.getResources("AndroidManifest.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (res.hasMoreElements()) {
            String path = res.nextElement().getPath();
            int hyphen = path.lastIndexOf('-');
            if (hyphen == -1) {
                continue;
            }
            File data = new File(root, new File(path.substring(0, hyphen)).getName());
            if (data.exists()) {
                File dir = new File(data, "cache/dexmockito");
                if (dir.isDirectory() && dir.canRead() && dir.canWrite() || dir.mkdirs()) {
                    return dir;
                }
            }
        }
        throw new RuntimeException("Cannot access Dexmockito cache directory");
    }

}
