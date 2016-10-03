package com.github.tmurakami.dexmockito;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.creation.bytebuddy.MockAccess;
import org.mockito.mock.MockCreationSettings;
import org.mockito.mock.SerializableMode;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.mock.SerializableMode.ACROSS_CLASSLOADERS;

@RunWith(Parameterized.class)
public class ByteBuddyMockClassGeneratorTest {

    @Mock
    MockCreationSettings<C> settings;
    @Mock
    ClassLoaderResolver resolver;

    private final SerializableMode serializableMode;
    private ByteBuddyMockClassGenerator target;

    public ByteBuddyMockClassGeneratorTest(SerializableMode serializableMode) {
        this.serializableMode = serializableMode;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        target = new ByteBuddyMockClassGenerator(resolver, ClassLoadingStrategy.Default.INJECTION);
    }

    @Parameterized.Parameters(name = "serializable={0}")
    public static SerializableMode[] parameters() {
        return SerializableMode.values();
    }

    @Test
    public void testGenerateMockClass() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        given(resolver.resolveClassLoader(settings)).willReturn(classLoader);
        given(settings.getTypeToMock()).willReturn(C.class);
        given(settings.getExtraInterfaces()).willReturn(Collections.<Class<?>>singleton(I.class));
        given(settings.getSerializableMode()).willReturn(serializableMode);
        Class<?> c = target.generateMockClass(settings);
        assertTrue(C.class.isAssignableFrom(c));
        assertTrue(I.class.isAssignableFrom(c));
        assertTrue(MockAccess.class.isAssignableFrom(c));
        assertEquals(classLoader, c.getClassLoader());
        assertEquals(serializableMode == ACROSS_CLASSLOADERS, hasWriteReplaceMethod(c));
        Annotation[] annotations = c.getDeclaredAnnotations();
        assertEquals(1, annotations.length);
        assertTrue(annotations[0] instanceof A);
        Field serialVersionUID = c.getDeclaredField("serialVersionUID");
        serialVersionUID.setAccessible(true);
        assertEquals(42L, serialVersionUID.get(null));
        Annotation[] methodAnnotations = c.getMethod("doIt").getDeclaredAnnotations();
        assertEquals(1, methodAnnotations.length);
        assertEquals(A.class, methodAnnotations[0].annotationType());
    }

    private static boolean hasWriteReplaceMethod(Class<?> c) {
        try {
            return Modifier.isPublic(c.getDeclaredMethod("writeReplace").getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface A {
    }

    private interface I {
    }

    @A
    private static class C {
        @SuppressWarnings("unused")
        @A
        public void doIt() {
        }
    }

}
