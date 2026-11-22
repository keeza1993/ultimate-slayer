# Development

Use JDK 17 and the Gradle wrapper. The source targets Java 11.

```sh
./gradlew build
./gradlew run
```

On Windows, use `gradlew.bat`. The development launcher uses developer mode. Safe mode is optional and disables GPU: use `gradlew.bat run --args="--developer-mode --safe-mode"` only when troubleshooting.

Jagex account setup: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

See [VALIDATION.md](VALIDATION.md) for validation coverage and limitations.
