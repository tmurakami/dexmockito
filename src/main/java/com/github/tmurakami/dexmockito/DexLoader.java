package com.github.tmurakami.dexmockito;

import java.io.Closeable;

interface DexLoader extends BiFunction<String, ClassLoader, Class>, Closeable {
}
