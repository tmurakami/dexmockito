package com.github.tmurakami.dexmockito;

public interface Function<T, R> {
    R apply(T t);
}