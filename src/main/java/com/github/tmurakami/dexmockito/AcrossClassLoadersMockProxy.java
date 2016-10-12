package com.github.tmurakami.dexmockito;

import net.bytebuddy.implementation.bind.annotation.This;

import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.settings.CreationSettings;
import org.mockito.internal.util.MockUtil;
import org.mockito.mock.MockCreationSettings;
import org.mockito.mock.SerializableMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public final class AcrossClassLoadersMockProxy implements Externalizable {

    private transient Object mock;

    public AcrossClassLoadersMockProxy() {
        this(null);
    }

    public AcrossClassLoadersMockProxy(@This Object mock) {
        this.mock = mock;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        readExternal(in, ((DexMockitoMockMaker) Plugins.getMockMaker()).getHelper());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeExternal(out, MockUtil.getMockSettings(mock));
    }

    Object readResolve() {
        return mock;
    }

    void readExternal(ObjectInput in, DexMockitoMockMakerHelper helper)
            throws IOException, ClassNotFoundException {
        String name = (String) in.readObject();
        @SuppressWarnings("unchecked")
        MockCreationSettings<?> settings = new CreationSettings()
                .setTypeToMock((Class<?>) in.readObject())
                .setExtraInterfaces((Set<Class<?>>) in.readObject())
                .setSerializableMode(SerializableMode.ACROSS_CLASSLOADERS);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        for (int l; (l = in.read(buffer)) != -1; ) {
            baos.write(buffer, 0, l);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        mock = new InternalObjectInputStream(bais, helper, name, settings).readObject();
    }

    void writeExternal(ObjectOutput out, MockCreationSettings<?> settings) throws IOException {
        Object o = mock;
        out.writeObject(o.getClass().getName());
        out.writeObject(settings.getTypeToMock());
        out.writeObject(settings.getExtraInterfaces());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        ObjectOutputStream oos = new InternalObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        out.write(baos.toByteArray());
        out.flush();
    }

    private static class InternalObjectInputStream extends ObjectInputStream {

        private final DexMockitoMockMakerHelper helper;
        private final String name;
        private final MockCreationSettings<?> settings;

        InternalObjectInputStream(InputStream in,
                                  DexMockitoMockMakerHelper helper,
                                  String name,
                                  MockCreationSettings<?> settings) throws IOException {
            super(in);
            this.helper = helper;
            this.name = name;
            this.settings = settings;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return desc.getName().equals(name) ? helper.resolveMockClass(desc, settings) : super.resolveClass(desc);
        }

    }

    private static class InternalObjectOutputStream extends ObjectOutputStream {

        InternalObjectOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object o) throws IOException {
            return o instanceof AcrossClassLoadersMockProxy ? ((AcrossClassLoadersMockProxy) o).mock : super.replaceObject(o);
        }

    }

}
