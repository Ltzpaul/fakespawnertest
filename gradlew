#!/usr/bin/env sh
set -eu
GRADLE_VERSION=8.14.2
GRADLE_HOME="${HOME}/.gradle/fake-spawner/gradle-${GRADLE_VERSION}"
GRADLE_ZIP="${HOME}/.gradle/fake-spawner/gradle-${GRADLE_VERSION}-bin.zip"
if [ ! -x "${GRADLE_HOME}/bin/gradle" ]; then
  mkdir -p "${HOME}/.gradle/fake-spawner"
  echo "Downloading Gradle ${GRADLE_VERSION} ..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "${GRADLE_ZIP}"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "${GRADLE_ZIP}" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Install curl or wget to bootstrap Gradle." >&2
    exit 1
  fi
  unzip -q -o "${GRADLE_ZIP}" -d "${HOME}/.gradle/fake-spawner"
fi
exec "${GRADLE_HOME}/bin/gradle" "$@"
