package com.github.tmurakami.dexmockito;

import net.bytebuddy.implementation.bind.annotation.This;

import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.settings.CreationSettings;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.mock.MockCreationSettings;
import org.mockito.mock.SerializableMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public final class AcrossClassLoadersMockProxy implements Serializable {

    private static final long serialVersionUID = -854526631339770616L;

    transient Object mock;

    public AcrossClassLoadersMockProxy(@This Object mock) {
        this.mock = mock;
    }

    private Object readResolve() {
        return mock;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        DexMockitoMockMaker mockMaker = (DexMockitoMockMaker) Plugins.getMockMaker();
        String name = (String) in.readObject();
        @SuppressWarnings("unchecked")
        MockCreationSettings<?> settings = new CreationSettings()
                .setTypeToMock((Class<?>) in.readObject())
                .setExtraInterfaces((Set<Class<?>>) in.readObject())
                .setSerializableMode(SerializableMode.ACROSS_CLASSLOADERS);
        ObjectInputStream ois = new InternalObjectInputStream(in, mockMaker, name, settings);
        try {
            mock = ois.readObject();
        } finally {
            IOUtil.closeQuietly(ois);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(mock.getClass().getName());
        MockCreationSettings<?> settings = MockUtil.getMockSettings(mock);
        out.writeObject(settings.getTypeToMock());
        out.writeObject(settings.getExtraInterfaces());
        ObjectOutputStream oos = new InternalObjectOutputStream(out);
        try {
            oos.writeObject(mock);
        } finally {
            IOUtil.closeQuietly(oos);
        }
    }

    private static class InternalObjectOutputStream extends ObjectOutputStream {

        InternalObjectOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) throws IOException {
            if (obj instanceof AcrossClassLoadersMockProxy) {
                return ((AcrossClassLoadersMockProxy) obj).mock;
            } else {
                return super.replaceObject(obj);
            }
        }

    }

    private static class InternalObjectInputStream extends ObjectInputStream {

        private final DexMockitoMockMaker mockMaker;
        private final String name;
        private final MockCreationSettings<?> settings;

        InternalObjectInputStream(InputStream in,
                                  DexMockitoMockMaker mockMaker,
                                  String name,
                                  MockCreationSettings<?> settings) throws IOException {
            super(in);
            this.mockMaker = mockMaker;
            this.name = name;
            this.settings = settings;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            if (desc.getName().equals(name)) {
                return mockMaker.resolveMockClass(desc, settings);
            } else {
                return super.resolveClass(desc);
            }
        }

    }

}
