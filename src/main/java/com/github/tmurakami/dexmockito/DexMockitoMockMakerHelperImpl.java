package com.github.tmurakami.dexmockito;

import android.os.Build;

import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.mock.MockCreationSettings;

import java.io.ObjectStreamClass;
import java.lang.reflect.Field;

final class DexMockitoMockMakerHelperImpl extends DexMockitoMockMakerHelper {

    private static final Field NAME;

    static {
        String name = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? "className" : "name";
        try {
            NAME = ObjectStreamClass.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
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
    public Class resolveMockClass(ObjectStreamClass desc, MockCreationSettings<?> settings) {
        Class<?> c = generateMockClass(settings);
        String name = c.getName();
        if (!name.equals(desc.getName())) {
            FieldSetter.setField(desc, NAME, name);
        }
        return c;
    }

}
