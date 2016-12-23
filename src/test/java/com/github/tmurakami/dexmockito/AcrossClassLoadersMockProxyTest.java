package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.mock.MockCreationSettings;
import org.mockito.mock.SerializableMode;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@RunWith(MockitoJUnitRunner.class)
public class AcrossClassLoadersMockProxyTest {

    @Mock
    ObjectInput in;
    @Mock
    ObjectOutput out;
    @Mock
    DexMockitoMockMakerHelper helper;
    @Mock
    MockCreationSettings settings;

    @Test
    public void testReadExternal() throws IOException, ClassNotFoundException {
        given(in.readObject()).willReturn(C.class.getName(), C.class, Collections.singleton(I.class));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new C());
        oos.close();
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        given(in.read(any(byte[].class))).willAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return bais.read(invocation.<byte[]>getArgument(0));
            }
        });
        given(helper.resolveMockClass(
                argThat(new ArgumentMatcher<ObjectStreamClass>() {
                    @Override
                    public boolean matches(ObjectStreamClass argument) {
                        return argument.getName().equals(C.class.getName());
                    }
                }),
                argThat(new ArgumentMatcher<MockCreationSettings<?>>() {
                    @Override
                    public boolean matches(MockCreationSettings<?> argument) {
                        Set<Class<?>> extraInterfaces = argument.getExtraInterfaces();
                        return argument.getTypeToMock().equals(C.class)
                                && extraInterfaces.size() == 1
                                && extraInterfaces.contains(I.class)
                                && argument.getSerializableMode() == SerializableMode.ACROSS_CLASSLOADERS;
                    }
                }))).willReturn(C.class);
        AcrossClassLoadersMockProxy proxy = new AcrossClassLoadersMockProxy();
        proxy.readExternal(in, helper);
        assertTrue(proxy.readResolve() instanceof C);
    }

    @Test
    public void testWriteExternal() throws IOException, ClassNotFoundException {
        C c = new C();
        given(settings.getTypeToMock()).willReturn(C.class);
        given(settings.getExtraInterfaces()).willReturn(Collections.singleton(I.class));
        new AcrossClassLoadersMockProxy(c).writeExternal(out, settings);
        InOrder inOrder = inOrder(out);
        then(out).should(inOrder).writeObject(C.class.getName());
        then(out).should(inOrder).writeObject(C.class);
        then(out).should(inOrder).writeObject(Collections.singleton(I.class));
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        then(out).should(inOrder).write(captor.capture());
        assertTrue(new ObjectInputStream(new ByteArrayInputStream(captor.getValue())).readObject() instanceof C);
    }

    static class C implements Serializable {
    }

    interface I {
    }

}
