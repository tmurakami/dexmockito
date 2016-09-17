package com.github.tmurakami.dexmockito;

import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.creation.bytebuddy.ByteBuddyMockMaker;
import org.mockito.internal.creation.instance.Instantiator;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockMaker;
import org.objenesis.ObjenesisStd;

public final class DexMockitoMockMaker implements MockMaker {

    private final MockMaker delegate;
    private final MockClassMaker mockClassMaker;

    public DexMockitoMockMaker() {
        this(new ObjenesisStd(false).newInstance(ByteBuddyMockMaker.class), DefaultMockClassMakerFactory.INSTANCE.get());
    }

    private DexMockitoMockMaker(MockMaker delegate, MockClassMaker mockClassMaker) {
        this.delegate = delegate;
        this.mockClassMaker = mockClassMaker;
    }

    @Override
    public <T> T createMock(MockCreationSettings<T> settings, MockHandler handler) {
        Class<?> c = mockClassMaker.apply(settings);
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

}
