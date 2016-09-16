package org.mockito.internal.creation.bytebuddy;

import com.github.tmurakami.dexmockito.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.creation.bytebuddy.ByteBuddyCrossClassLoaderSerializationSupport.CrossClassLoaderSerializableMock;
import org.mockito.internal.creation.bytebuddy.MockMethodInterceptor.MockAccess;
import org.mockito.mock.MockCreationSettings;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;

@RunWith(Parameterized.class)
public class ByteBuddyMockClassMakerTest {

    @Mock
    MockCreationSettings<C> settings;
    @Mock
    Function<MockCreationSettings<?>, ClassLoader> classLoaderResolver;

    private ByteBuddyMockClassMaker target;
    private final boolean serializable;

    public ByteBuddyMockClassMakerTest(boolean serializable) {
        this.serializable = serializable;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        target = new ByteBuddyMockClassMaker(new ByteBuddy(), classLoaderResolver, ClassLoadingStrategy.Default.INJECTION);
    }

    @Parameterized.Parameters(name = "serializable={0}")
    public static Iterable<Boolean> parameters() {
        return Arrays.asList(false, true);
    }

    @Test
    public void testGenerate() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException {
        given(settings.getTypeToMock()).willReturn(C.class);
        given(settings.getExtraInterfaces()).willReturn(Collections.<Class<?>>singleton(I.class));
        given(settings.isSerializable()).willReturn(serializable);
        ClassLoader classLoader = getClass().getClassLoader();
        given(classLoaderResolver.apply(any(MockCreationSettings.class))).willReturn(classLoader);
        Class<?> c = target.apply(settings);
        assertTrue(C.class.isAssignableFrom(c));
        assertTrue(I.class.isAssignableFrom(c));
        assertTrue(MockAccess.class.isAssignableFrom(c));
        assertEquals(classLoader, c.getClassLoader());
        assertEquals(serializable, CrossClassLoaderSerializableMock.class.isAssignableFrom(c));
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

    @Retention(RetentionPolicy.RUNTIME)
    @interface A {
    }

    interface I {
    }

    @SuppressWarnings("WeakerAccess")
    @A
    static class C {
        @SuppressWarnings("unused")
        @A
        public void doIt() {
        }
    }

}
