package com.github.tmurakami.dexmockito;

import org.mockito.mock.MockCreationSettings;

import java.io.ObjectStreamClass;

interface DexMockitoMockMakerHelper {

    Class generateMockClass(MockCreationSettings<?> settings);

    Class resolveMockClass(ObjectStreamClass desc, MockCreationSettings<?> settings);

}
