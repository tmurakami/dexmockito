package com.github.tmurakami.dexmockito;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@RunWith(Parameterized.class)
public class DexCacheDirTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock
    ClassLoader classLoader;
    @Mock
    Enumeration<URL> resources;

    private final String apkPath;

    public DexCacheDirTest(String apkPath) {
        this.apkPath = apkPath;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Parameterized.Parameters(name = "apkPath={0}")
    public static Iterable<String> parameters() {
        return Arrays.asList(
                "/data/app/a.b.c-1.apk",
                "/data/app/a.b.c-1/base.apk"
        );
    }

    @Test
    public void testGet() throws IOException {
        given(classLoader.getResources("AndroidManifest.xml")).willReturn(resources);
        given(resources.hasMoreElements()).willReturn(true, false);
        URL url = new URL("jar:file:" + apkPath + "!/AndroidManifest.xml");
        given(resources.nextElement()).willReturn(url).willThrow(new NoSuchElementException());
        folder.newFolder("a.b.c");
        File root = folder.getRoot();
        assertEquals(new File(root, "a.b.c/cache/dexmockito"), DexCacheDir.get(root, classLoader));
    }

}
