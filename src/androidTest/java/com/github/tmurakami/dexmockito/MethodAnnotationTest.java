package com.github.tmurakami.dexmockito;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.mockito.Mockito.mock;

public class MethodAnnotationTest extends TestCase {

    public void testMethodAnnotation() throws NoSuchMethodException {
        Class<?> c = mock(C.class).getClass();
        Annotation[] annotations = c.getDeclaredMethod("doIt").getDeclaredAnnotations();
        assertEquals(1, annotations.length);
        assertSame(A2.class, annotations[0].annotationType());
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface A1 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface A2 {
        @SuppressWarnings("unused")
        A1[] value() default {};
    }

    private static class C {
        @SuppressWarnings("unused")
        @A2
        void doIt() {
        }
    }

}
