package com.github.tmurakami.dexmockito;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.StreamDrainer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DexClassLoadingStrategyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock
    DexFileLoader dexFileLoader;
    @Mock
    DexFile dexFile;

    @Captor
    ArgumentCaptor<File> sourceCaptor;
    @Captor
    ArgumentCaptor<File> outputCaptor;

    private File cacheDir;
    private DexClassLoadingStrategy target;

    @Before
    public void setUp() throws IOException {
        cacheDir = folder.newFolder();
        target = new DexClassLoadingStrategy(cacheDir, dexFileLoader);
    }

    @Test
    public void testLoad() throws Exception {
        given(dexFileLoader.load(any(File.class), any(File.class))).willReturn(dexFile);
        Class[] classes = {A.class, B.class, C.class};
        Map<TypeDescription, byte[]> bytecodeMap = new HashMap<>();
        Map<TypeDescription, Class> classMap = new HashMap<>();
        for (Class<?> c : classes) {
            TypeDescription td = mock(TypeDescription.class);
            given(td.getName()).willReturn(c.getName());
            InputStream in = c.getResourceAsStream('/' + c.getName().replace('.', '/') + ".class");
            try {
                bytecodeMap.put(td, StreamDrainer.DEFAULT.drain(in));
            } finally {
                IOUtil.closeQuietly(in);
            }
            classMap.put(td, c);
        }
        ClassLoader classLoader = getClass().getClassLoader();
        assertEquals(classMap, target.load(classLoader, bytecodeMap));
        for (Class<?> c : classes) {
            then(dexFile).should().loadClass(c.getName(), classLoader);
        }
        then(dexFile).should().close();
        then(dexFileLoader).should().load(sourceCaptor.capture(), outputCaptor.capture());
        File[] files = {sourceCaptor.getValue(), outputCaptor.getValue()};
        for (File f : files) {
            assertFalse(f.exists());
            assertEquals(cacheDir, f.getParentFile());
        }
        assertTrue(files[0].getAbsolutePath().endsWith(".jar"));
        assertTrue(files[1].getAbsolutePath().endsWith(".dex"));
    }

    private static class A {
    }

    private static class B {
    }

    private static class C {
    }

}
