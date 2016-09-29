package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

interface ClassLoaderResolver {
    ClassLoader resolve(MockCreationSettings<?> settings);
}
