package com.github.tmurakami.dexmockito;

import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

public class DexClassLoadingStrategyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock
    TypeDescription typeDescription;

    @Mock
    BiFunction<File, File, BiFunction<String, ClassLoader, Class>> dexClassLoaderFactory;

    @Mock
    BiFunction<String, ClassLoader, Class> dexClassLoader;

    private DexClassLoadingStrategy target;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        target = new DexClassLoadingStrategy(new DexOptions(), new CfOptions(), folder.newFolder(), dexClassLoaderFactory);
    }

    @Test
    public void testLoad() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        given(dexClassLoader.apply(anyString(), eq(classLoader))).willAnswer(new Answer<Class>() {
            @Override
            public Class answer(InvocationOnMock invocation) throws Throwable {
                return Class.forName(invocation.<String>getArgument(0), false, invocation.<ClassLoader>getArgument(1));
            }
        });
        given(dexClassLoaderFactory.apply(any(File.class), any(File.class))).willReturn(dexClassLoader);
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
    }

    private static class A {
    }

    private static class B {
    }

    private static class C {
    }

}
