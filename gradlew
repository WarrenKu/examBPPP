#!/bin/sh
set -e
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
elif [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
    echo "Gradle not found in PATH, installing or running fallback..."
    exec gradle "$@"
fi
