#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
if [ -n "${JAVA_HOME:-}" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAVA="$JAVA_HOME/bin/java"
    KEYTOOL="$JAVA_HOME/bin/keytool"
else
    JAVAC=javac
    JAVA=java
    KEYTOOL=keytool
fi
BUILD_DIR=$(mktemp -d)
trap 'rm -rf "$BUILD_DIR"' EXIT HUP INT TERM
"$JAVAC" -encoding UTF-8 -d "$BUILD_DIR" \
    src/main/java/sort/QuickSort.java src/main/java/sort/MergeSort.java \
    src/main/java/thread/Interrupt.java src/main/java/thread/producercostomer/*.java \
    src/main/java/util/HttpsUtils.java src/main/java/util/HttpsParam.java \
    src/main/java/luairan/http/client/HttpsUtils.java src/main/java/luairan/http/client/HttpsParam.java \
    src/main/java/luairan/textserver/nio/Server.java tests/RegressionTest.java
"$KEYTOOL" -genkeypair -alias test -keyalg RSA -keysize 2048 -validity 2 \
    -dname "CN=wrong.example" -ext "SAN=dns:wrong.example" \
    -storetype PKCS12 -keystore "$BUILD_DIR/test.p12" \
    -storepass changeit -keypass changeit >/dev/null 2>&1
"$JAVA" -ea -cp "$BUILD_DIR" RegressionTest "$BUILD_DIR/test.p12"
