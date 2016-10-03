package com.github.tmurakami.dexmockito;

import android.content.Context;

import org.mockito.internal.util.io.IOUtil;
import org.mockito.mock.SerializableMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

import dalvik.system.DexClassLoader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class SerializationTest extends BaseAndroidTestCase {

    public void testSerialization() throws Exception {
        Object o = mock(C.class, withSettings().serializable(SerializableMode.ACROSS_CLASSLOADERS));
        assertNotSame(o.getClass(), deserialize(serialize(o)).getClass());
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
            oos.writeObject(o);
        } finally {
            IOUtil.closeQuietly(oos);
        }
        return baos.toByteArray();
    }

    private Object deserialize(byte[] serialData) throws Exception {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Context context = getContext();
        Class<?> thisClass = getClass();
        String sourcePath = context.getApplicationInfo().sourceDir;
        File dexDir = new File(context.getCacheDir(), thisClass.getSimpleName().toLowerCase(Locale.US));
        assertTrue(dexDir.mkdir());
        try {
            ClassLoader loader = new DexClassLoader(sourcePath, dexDir.getCanonicalPath(), null, ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> c = Class.forName(thisClass.getName(), false, loader);
            return c.getDeclaredMethod("readObject", byte[].class).invoke(null, new Object[]{serialData});
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            FileUtils.forceDelete(dexDir);
        }
    }

    @SuppressWarnings("unused")
    public static Object readObject(byte[] serialData) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialData));
        try {
            return ois.readObject();
        } finally {
            IOUtil.closeQuietly(ois);
        }
    }

    @SuppressWarnings("WeakerAccess")
    static class C {
    }

}
