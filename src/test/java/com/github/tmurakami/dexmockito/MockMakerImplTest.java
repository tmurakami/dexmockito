package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.mockito.runners.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

import java.io.ObjectStreamClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class MockMakerImplTest {

    @Mock
    MockMaker delegate;
    @Mock
    MockClassGenerator generator;
    @Mock
    FieldSetter objectStreamClassNameFieldSetter;
    @Mock
    MockCreationSettings<C> settings;
    @Mock
    MockHandler handler;
    @Mock
    MockMaker.TypeMockability typeMockability;

    @InjectMocks
    MockMakerImpl target;

    @Test
    public void testCreateMock() {
        Class<C> c = C.class;
        given(generator.generateMockClass(settings)).willReturn(c);
        given(settings.getTypeToMock()).willReturn(c);
        C mock = target.createMock(settings, handler);
        then(delegate).should().resetMock(mock, handler, settings);
    }

    @Test
    public void testGetHandler() {
        Object o = new Object();
        given(delegate.getHandler(o)).willReturn(handler);
        assertEquals(handler, target.getHandler(o));
    }

    @Test
    public void testResetMock() {
        Object o = new Object();
        target.resetMock(o, handler, settings);
        then(delegate).should().resetMock(o, handler, settings);
    }

    @Test
    public void testIsTypeMockable() {
        given(delegate.isTypeMockable(C.class)).willReturn(typeMockability);
        assertEquals(typeMockability, target.isTypeMockable(C.class));
    }

    @Test
    public void testResolveMockClass() throws IllegalAccessException {
        given(generator.generateMockClass(settings)).willReturn(C.class);
        ObjectStreamClass desc = spy(new ObjenesisStd(false).newInstance(ObjectStreamClass.class));
        willReturn("a").given(desc).getName();
        assertSame(C.class, target.resolveMockClass(desc, settings));
        then(objectStreamClassNameFieldSetter).should().setField(desc, C.class.getName());
    }

    private static class C {
    }

}
