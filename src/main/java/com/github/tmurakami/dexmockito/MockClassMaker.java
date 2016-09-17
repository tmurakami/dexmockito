package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

public interface MockClassMaker extends Function<MockCreationSettings<?>, Class> {
    interface Factory extends Supplier<MockClassMaker> {
    }
}
