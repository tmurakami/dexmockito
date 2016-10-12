package com.github.tmurakami.dexmockito;

import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.bytebuddy.SubclassByteBuddyMockMaker;
import org.mockito.internal.creation.instance.Instantiator;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.objenesis.ObjenesisStd;

public final class DexMockitoMockMaker implements MockMaker {

    private final MockMaker delegate;
    private final DexMockitoMockMakerHelper helper;

    public DexMockitoMockMaker() {
        this(new ObjenesisStd(false).newInstance(SubclassByteBuddyMockMaker.class), DexMockitoMockMakerHelperFactory.newDexMockitoMockMakerHelper());
    }

    private DexMockitoMockMaker(MockMaker delegate, DexMockitoMockMakerHelper helper) {
        this.delegate = delegate;
        this.helper = helper;
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<?> c = helper.generateMockClass(settings);
        Instantiator instantiator = Plugins.getInstantiatorProvider().getInstantiator(settings);
        T mock = settings.getTypeToMock().cast(instantiator.newInstance(c));
        resetMock(mock, handler, settings);
        return mock;
    }

    @Override
    public MockHandler getHandler(Object mock) {
        return delegate.getHandler(mock);
    }

    @Override
    public void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings) {
        delegate.resetMock(mock, newHandler, settings);
    }

    @Override
    public TypeMockability isTypeMockable(Class<?> type) {
        return delegate.isTypeMockable(type);
    }

    DexMockitoMockMakerHelper getHelper() {
        return helper;
    }

}
