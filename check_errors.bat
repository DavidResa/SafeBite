@echo off
./gradlew :app:compileDebugKotlin --no-daemon --console=plain --quiet > errors.txt 2>&1
type errors.txt
