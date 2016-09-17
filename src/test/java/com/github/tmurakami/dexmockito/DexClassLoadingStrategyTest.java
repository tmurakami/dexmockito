package com.github.tmurakami.dexmockito;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.StreamDrainer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexFile;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public class DexClassLoadingStrategyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock
    TypeDescription typeDescription;
    @Mock
    DexClassLoadingStrategy.DexFileLoader dexLoaderFactory;
    @Mock
    DexFile dexFile;

    private DexClassLoadingStrategy target;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        target = new DexClassLoadingStrategy(folder.newFolder(), dexLoaderFactory);
    }

    @Test
    public void testLoad() throws IOException, ClassNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        given(dexFile.loadClass(anyString(), eq(classLoader))).willAnswer(new Answer<Class>() {
            @Override
            public Class answer(InvocationOnMock invocation) throws Throwable {
                return Class.forName(invocation.<String>getArgument(0), false, invocation.<ClassLoader>getArgument(1));
            }
        });
        given(dexLoaderFactory.loadDex(endsWith(".jar"), endsWith(".dex"), eq(0))).willReturn(dexFile);
        Map<TypeDescription, byte[]> types = new HashMap<>();
        Map<TypeDescription, Class> classes = new HashMap<>();
        for (Class<?> c : new Class[]{A.class, B.class, C.class}) {
            TypeDescription td = mock(TypeDescription.class);
            given(td.getName()).willReturn(c.getName());
            InputStream in = c.getResourceAsStream('/' + c.getName().replace('.', '/') + ".class");
            try {
                types.put(td, StreamDrainer.DEFAULT.drain(in));
            } finally {
                IOUtil.closeQuietly(in);
            }
            classes.put(td, c);
        }
        assertEquals(classes, target.load(classLoader, types));
        then(dexFile).should().close();
    }

    private static class A {
    }

    private static class B {
    }

    private static class C {
    }

}
