// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    kotlin("android") version "1.9.0" apply false
    id("com.android.application") version "8.1.1" apply false
}


buildscript {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
    // other buildscript configurations
}
