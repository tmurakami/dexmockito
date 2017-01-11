package com.github.tmurakami.dexmockito;

import java.io.File;

import static org.mockito.Mockito.mock;

public class CacheDirTest extends BaseAndroidTestCase {

    public void testCacheDirExists() {
        File cacheDir = new File(getContext().getCacheDir(), "dexmockito");
        assertTrue(!cacheDir.exists() || forceDelete(cacheDir));
        mock(C.class);
        assertTrue(cacheDir.isDirectory());
        assertTrue(cacheDir.canRead());
        assertTrue(cacheDir.canWrite());
    }

    private static boolean forceDelete(File file) {
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

    private static class C {
    }

}
