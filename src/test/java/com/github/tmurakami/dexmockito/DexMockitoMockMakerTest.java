package com.github.tmurakami.dexmockito;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@RunWith(MockitoJUnitRunner.class)
public class DexMockitoMockMakerTest {

    @Mock
    MockMaker delegate;
    @Mock
    MockClassGenerator mockClassGenerator;
    @Mock
    MockCreationSettings<C> settings;
    @Mock
    MockHandler handler;
    @Mock
    MockMaker.TypeMockability typeMockability;

    @InjectMocks
    DexMockitoMockMaker target;

    @Test
    public void testCreateMock() {
        Class<C> c = C.class;
        given(mockClassGenerator.generate(settings)).willReturn(c);
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

    private static class C {
    }

}
