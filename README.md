# DexMockito

[![CircleCI](https://circleci.com/gh/tmurakami/dexmockito.svg?style=shield)](https://circleci.com/gh/tmurakami/dexmockito)
[![Release](https://jitpack.io/v/tmurakami/dexmockito.svg)](https://jitpack.io/#tmurakami/dexmockito)

A library that provides [Mockito2](https://github.com/mockito/mockito) MockMaker for Android.

## Installation

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    androidTestCompile 'com.github.tmurakami:dexmockito:x.y.z'
}
```

## Notice

This library includes [com.google.android.tools:dx:1.7](https://bintray.com/bintray/jcenter/com.google.android.tools%3Adx) that has been repackaged using [Jar Jar Links](https://code.google.com/archive/p/jarjar/).
