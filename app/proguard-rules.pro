# JGit 依赖大量反射和 SPI(ServiceLoader)，Transport/SSH 通过
# META-INF/services 注册。类被重命名会导致 clone/pull/push/SSH 运行时找不到实现。
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# Apache MINA sshd — 同样通过 ServiceLoader 与反射实例化
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**

# Bouncy Castle JCA Provider — 经反射注册到 Security，混淆后 key 解析必挂
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# JGit ssh.apache 传递引入的 SLF4J，保留避免运行时 Logger 缺失
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# 匿名/静态内部类与泛型签名、注解，供反射与类型解析使用
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exceptions