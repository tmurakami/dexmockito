package com.github.tmurakami.dexmockito;

import android.os.Build;

import org.mockito.mock.MockCreationSettings;

import java.io.ObjectStreamClass;
import java.lang.reflect.Field;

final class DexMockitoMockMakerHelperImpl implements DexMockitoMockMakerHelper {

    private static final Field NAME;

    static {
        String name = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? "className" : "name";
        try {
            NAME = ObjectStreamClass.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private final MockClassGenerator generator;

    DexMockitoMockMakerHelperImpl(MockClassGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Class generateMockClass(MockCreationSettings<?> settings) {
        return generator.generateMockClass(settings);
    }

    @Override
    public void setName(ObjectStreamClass desc, String name) {
        NAME.setAccessible(true);
        try {
            NAME.set(desc, name);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
