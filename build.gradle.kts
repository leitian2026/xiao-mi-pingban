plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.lsparanoid) apply false
}

// fan.miuix:haptic 1.0.6 已从 ReChronoRain 的 GitHub Packages 中消失（现存最早版本为 1.0.10.5.2），
// 它只是 fan.miuix:folme:1.0.6 的传递依赖，源码中没有直接使用，这里全局排除以避免依赖解析失败。
allprojects {
    configurations.configureEach {
        exclude(group = "fan.miuix", module = "haptic")
    }
}
