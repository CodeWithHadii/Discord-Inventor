# Repackage classes to avoid conflicts
-repackageclasses com.bosonshiggs.discordinventor.repacked

# Ignore duplicate classes from twitter4j
-keep class twitter4j.** { *; }
-dontwarn twitter4j.**
-dontnote twitter4j.**

# Ignore unresolved references
-dontwarn javax.management.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.log4j.**
-dontwarn com.google.appengine.**

# Keep SLF4J unmodified
-keep class org.slf4j.** { *; }
-keep interface org.slf4j.Logger { *; }
