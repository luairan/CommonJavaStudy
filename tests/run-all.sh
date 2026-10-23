#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
MVN=${MVN:-mvn}
"$MVN" -q test-compile dependency:build-classpath -Dmdep.outputFile=target/dependency-classpath.txt
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
CLASSPATH="target/classes:$(cat target/dependency-classpath.txt)"
"$JAVAC" -encoding UTF-8 -cp "$CLASSPATH" -d "$BUILD_DIR" tests/RegressionTest.java tests/MoreRegressionTest.java
"$KEYTOOL" -genkeypair -alias test -keyalg RSA -keysize 2048 -validity 2 \
    -dname "CN=wrong.example" -ext "SAN=dns:wrong.example" \
    -storetype PKCS12 -keystore "$BUILD_DIR/test.p12" \
    -storepass changeit -keypass changeit >/dev/null 2>&1
"$JAVA" -ea -Djava.awt.headless=true -cp "$BUILD_DIR:$CLASSPATH" RegressionTest "$BUILD_DIR/test.p12"
"$JAVA" -ea -Djava.awt.headless=true -cp "$BUILD_DIR:$CLASSPATH" MoreRegressionTest
