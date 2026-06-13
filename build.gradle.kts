plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // 👇 请确保您的 Project 级 gradle 文件里有这一行！
    id("com.google.gms.google-services") version "4.4.1" apply false
}