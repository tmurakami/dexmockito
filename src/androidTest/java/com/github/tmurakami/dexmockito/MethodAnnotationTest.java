package com.github.tmurakami.dexmockito;

import junit.framework.TestCase;

import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MethodAnnotationTest extends TestCase {

    public void testMethodAnnotation() throws NoSuchMethodException {
        Class<?> c = Mockito.mock(C.class).getClass();
        Annotation[] annotations = c.getMethod("doIt").getDeclaredAnnotations();
        assertEquals(1, annotations.length);
        assertEquals(A2.class, annotations[0].annotationType());
    }

    @SuppressWarnings("WeakerAccess")
    @Retention(RetentionPolicy.RUNTIME)
    public @interface A1 {
    }

    @SuppressWarnings("WeakerAccess")
    @Retention(RetentionPolicy.RUNTIME)
    public @interface A2 {
        @SuppressWarnings("unused")
        A1[] value() default {};
    }

    @SuppressWarnings("WeakerAccess")
    public static class C {
        @SuppressWarnings("unused")
        @A2
        public void doIt() {
        }
    }

}
