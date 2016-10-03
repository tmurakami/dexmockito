package com.github.tmurakami.dexmockito;

import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.utility.StreamDrainer;

import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.settings.CreationSettings;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.mock.MockCreationSettings;
import org.mockito.mock.SerializableMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public final class SerializableMockProxy implements Serializable {

    private static final long serialVersionUID = -854526631339770616L;

    transient Object mock;

    public SerializableMockProxy(@This Object mock) {
        this.mock = mock;
    }

    private Object readResolve() {
        return mock;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        MockClassResolver resolver = (MockClassResolver) Plugins.getMockMaker();
        String name = (String) in.readObject();
        @SuppressWarnings("unchecked")
        MockCreationSettings<?> settings = new CreationSettings()
                .setTypeToMock((Class<?>) in.readObject())
                .setExtraInterfaces((Set<Class<?>>) in.readObject())
                .setSerializableMode(SerializableMode.ACROSS_CLASSLOADERS);
        InputStream bais = new ByteArrayInputStream(StreamDrainer.DEFAULT.drain(in));
        ObjectInputStream ois = new InternalObjectInputStream(bais, resolver, name, settings);
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new InternalObjectOutputStream(baos);
        try {
            oos.writeObject(mock);
        } finally {
            IOUtil.closeQuietly(oos);
        }
        out.write(baos.toByteArray());
    }

    private static class InternalObjectOutputStream extends ObjectOutputStream {

        InternalObjectOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object o) throws IOException {
            return o instanceof SerializableMockProxy ? ((SerializableMockProxy) o).mock : super.replaceObject(o);
        }

    }

    private static class InternalObjectInputStream extends ObjectInputStream {

        private final MockClassResolver resolver;
        private final String name;
        private final MockCreationSettings<?> settings;

        InternalObjectInputStream(InputStream in,
                                  MockClassResolver resolver,
                                  String name,
                                  MockCreationSettings<?> settings) throws IOException {
            super(in);
            this.resolver = resolver;
            this.name = name;
            this.settings = settings;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return desc.getName().equals(name) ? resolver.resolveMockClass(desc, settings) : super.resolveClass(desc);
        }

    }

}
