package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

interface MockClassGenerator {
    Class generateMockClass(MockCreationSettings<?> settings);
}
