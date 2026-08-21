package com.example.git.data

import com.example.git.R

enum class ThemeMode(val raw: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark")
}

enum class BgPreset(val raw: String, val light: String, val dark: String, val labelRes: Int) {
    DEFAULT("default", "#FAFAFA", "#121212", R.string.default_bg),
    GRAY("gray", "#F3F3F2", "#1C1C1E", R.string.gray_bg),
    CREAM("cream", "#F8F3E9", "#1F1A12", R.string.cream_bg),
    SKY("sky", "#EFF4FA", "#101A2A", R.string.sky_bg),
    GREEN("green", "#EDF5F0", "#0E1F18", R.string.green_bg),
    ROSE("rose", "#FAF0F2", "#231419", R.string.rose_bg),
    DARK("dark", "#1E1E1E", "#0A0A0A", R.string.dark_bg),
}

enum class AccentPreset(val raw: String, val light: String, val dark: String, val labelRes: Int) {
    INDIGO("indigo", "#4A5BD6", "#AEB8FF", R.string.indigo_accent),
    BLUE("blue", "#1E6FD9", "#9FC7FF", R.string.blue_accent),
    TEAL("teal", "#0F8771", "#8AD9C6", R.string.teal_accent),
    RED("red", "#D33A3A", "#FFA6A6", R.string.red_accent),
    AMBER("amber", "#B87900", "#FFD78F", R.string.amber_accent),
    PURPLE("purple", "#8B3FD8", "#D6AFFF", R.string.purple_accent),
    FOREST("forest", "#2D7D46", "#7DD89A", R.string.forest_accent),
    ORANGE("orange", "#D97706", "#FDBA74", R.string.orange_accent),
}

enum class AppLanguage(val raw: String, val tag: String?) {
    SYSTEM("system", null),
    ZH("zh", "zh"),
    EN("en", "en"),
}

data class UiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val bgPreset: BgPreset = BgPreset.DEFAULT,
    val accentPreset: AccentPreset = AccentPreset.INDIGO,
    val language: AppLanguage = AppLanguage.ZH,
    val customBgPath: String? = null
)
