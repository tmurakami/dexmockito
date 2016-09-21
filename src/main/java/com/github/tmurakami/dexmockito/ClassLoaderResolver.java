package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

public interface ClassLoaderResolver {
    ClassLoader resolve(MockCreationSettings<?> settings);
}
