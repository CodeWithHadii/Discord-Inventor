# Ignora as classes duplicadas de twitter4j
-dontwarn twitter4j.**
-keep class twitter4j.** { *; }

# Ignora as referências não resolvidas
-dontwarn javax.management.**
-dontwarn org.apache.logging.log4j.**

# Mantém SLF4J sem modificações
-keep class org.slf4j.** { *; }
-keep interface org.slf4j.Logger { *; }

# Repackaging para evitar conflitos
-repackageclasses com.bosonshiggs.discordinventor.repacked

# Suprimir warnings sobre classes ausentes
-dontwarn javax.management.**
-dontwarn org.apache.log4j.**
-dontwarn twitter4j.**

# Ignorar referências dinâmicas
-dontnote twitter4j.**

# Ignorar classes referenciadas dinamicamente
-dontwarn com.google.appengine.**

