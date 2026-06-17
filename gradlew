#!/usr/bin/env bash
# Hermes wrapper: ensures JDK 21 is used (required by Java 21 toolchain)
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"
export PATH="$JAVA_HOME/bin:$PATH"
exec ~/.gradle/wrapper/dists/gradle-9.3.1-bin/gradle-9.3.1/bin/gradle "$@"
