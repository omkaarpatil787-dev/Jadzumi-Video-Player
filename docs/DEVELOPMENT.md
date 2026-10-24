# Developer guide

Run these commands from the repository root with JDK 17 and the Android SDK installed
(platform 36 and build-tools 35.0.0).

## Build & Verification

```bash
# Run unit tests and build the debug APK
./gradlew test assembleDebug --no-daemon
# With release signing configured below:
./gradlew assembleRelease --no-daemon
```

## Release signing

Debug builds alone use the checked-in development key. Release builds read these
environment variables, or the corresponding keys in an ignored root `keystore.properties`:

| Environment variable | Property |
| --- | --- |
| `ANDROID_KEYSTORE_FILE` | `storeFile` |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` |
| `ANDROID_KEY_ALIAS` | `keyAlias` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` |

`storeFile` is absolute or relative to the repository root. Missing credentials produce
an **unsigned** release, never a release signed with the debug key. Keep keys and
passwords outside Git. Create a key locally with interactive password prompts:

```sh
keytool -genkeypair -v -keystore "$HOME/jadzumi-release.jks" -storetype JKS -alias jadzumi-release -keyalg RSA -keysize 4096 -validity 10000
```

In GitHub Actions, add `RELEASE_KEYSTORE_BASE64` (the base64-encoded keystore),
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` as
repository secrets. CI builds and publishes a signed release only when all four are
present; otherwise it runs tests and produces the debug APK. Artifacts use `versionName`.
For GNU/Linux, `base64 -w 0 "$HOME/jadzumi-release.jks" > "$HOME/jadzumi-release.base64"`
writes the secret input to a local file without printing it. Protect and remove that
file after secure secret entry, and back up the keystore securely.

**Existing installations:** a new signing key cannot update APKs signed with the old
development key. Plan signing continuity or a reinstall/migration before replacing
the public APK. Increment `versionCode` for the next release. Publishing this source
does not by itself establish complete Corresponding Source for native dependencies;
see the remaining review tasks in [THIRD_PARTY_LICENSES.md](../THIRD_PARTY_LICENSES.md).
