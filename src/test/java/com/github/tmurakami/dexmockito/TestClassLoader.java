package com.github.tmurakami.dexmockito;

class TestClassLoader extends ClassLoader {
    @SuppressWarnings("unchecked")
    <T> Class<T> defineClass(String name, byte[] bytecode) {
        return (Class<T>) defineClass(name, bytecode, 0, bytecode.length);
    }
}
