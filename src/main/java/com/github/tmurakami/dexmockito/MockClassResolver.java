package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

import java.io.ObjectStreamClass;

interface MockClassResolver {
    Class<?> resolveMockClass(ObjectStreamClass desc, MockCreationSettings<?> settings);
}
