# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# JGit
-keep class org.eclipse.jgit.** { *; }
-keep class org.eclipse.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.eclipse.**
