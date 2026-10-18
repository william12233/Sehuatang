#!/bin/sh

# Gradle Wrapper 啟動器；由 Gradle 8.9 產生，保留在原始碼中確保 CI 可重現建置。
set -e

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)

if [ -n "${JAVA_HOME:-}" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=$(command -v java)
fi

if [ ! -x "$JAVA" ]; then
    echo "找不到 Java，請設定 JAVA_HOME。" >&2
    exit 1
fi

exec "$JAVA" ${GRADLE_OPTS:-} ${JAVA_OPTS:-} \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"
