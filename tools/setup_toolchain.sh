#!/usr/bin/env bash
# Assemble the offline Android build toolchain (no Google/Maven servers needed).
# Sources: GitHub (git clone + committed binaries), PyPI (aapt2) and the npm
# registry (Kotlin compiler). Everything is cached under ./toolchain/.
#
# Components:
#   jdk/               Zulu JDK 8 (luka-dev/jxe2jar, committed binaries)
#   android.jar        API 36 android.jar for compilation (Sable/android-platforms)
#   android-link.jar   API 34 android.jar for aapt2 link (valid resources.arsc)
#   aapt2/             Android resource compiler (PyPI wheel, linux binary)
#   dx.jar             dexer compiled from AOSP dx sources (JDK 8 compatible)
#   apksigner.jar      build-tools apksigner (warren-bank/print-apk-signature)
#   kotlinc/           Kotlin compiler 2.0.21 (npm package kotlin-compiler)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export TC="$ROOT/toolchain"
mkdir -p "$TC"

echo "==> JDK 8 (Zulu, committed in github repo luka-dev/jxe2jar)"
if [ ! -x "$TC/jdk/bin/javac" ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/luka-dev/jxe2jar "$TC/_jxe"
  git -C "$TC/_jxe" sparse-checkout set jvms/zulu8.78.0.19-ca-jdk8.0.412-linux_x64
  mv "$TC/_jxe/jvms/zulu8.78.0.19-ca-jdk8.0.412-linux_x64" "$TC/jdk"
  rm -rf "$TC/_jxe"
fi

echo "==> android.jar (API 36 classes + API 34 resources from Sable/android-platforms)"
if [ ! -f "$TC/android.jar" ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Sable/android-platforms "$TC/_plat"
  git -C "$TC/_plat" sparse-checkout set android-36 android-34
  cp "$TC/_plat/android-36/android.jar" "$TC/android.jar"
  cp "$TC/_plat/android-34/android.jar" "$TC/android-link.jar"
  rm -rf "$TC/_plat"
fi

echo "==> aapt2 (PyPI wheel, linux binary inside)"
if [ ! -x "$TC/aapt2/bin/Linux/aapt2" ]; then
  TC="$TC" python3 - <<'PY'
import json, os, urllib.request, zipfile
tc = os.environ["TC"]
j = json.load(urllib.request.urlopen("https://pypi.org/pypi/aapt2/json"))
url = j["urls"][0]["url"]
data = urllib.request.urlopen(url).read()
open("/tmp/_aapt2.whl", "wb").write(data)
zipfile.ZipFile("/tmp/_aapt2.whl").extractall(tc)
os.chmod(os.path.join(tc, "aapt2", "bin", "Linux", "aapt2"), 0o755)
PY
fi

echo "==> dx.jar (compiled from AOSP dx sources)"
if [ ! -f "$TC/dx.jar" ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/aosp-mirror/platform_dalvik "$TC/_dalvik"
  git -C "$TC/_dalvik" sparse-checkout set dx/src
  find "$TC/_dalvik/dx/src" -name "*.java" > "$TC/_dx_sources.txt"
  mkdir -p "$TC/_dxout"
  "$TC/jdk/bin/javac" -source 8 -target 8 -nowarn -classpath "$TC/android.jar" \
    -d "$TC/_dxout" @"$TC/_dx_sources.txt"
  (cd "$TC/_dxout" && "$TC/jdk/bin/jar" cf "$TC/dx.jar" com)
  rm -rf "$TC/_dalvik" "$TC/_dx_sources.txt" "$TC/_dxout"
fi

echo "==> apksigner.jar (build-tools 29.0.2, committed in warren-bank/print-apk-signature)"
if [ ! -f "$TC/apksigner.jar" ]; then
  git clone -q --depth 1 https://github.com/warren-bank/print-apk-signature "$TC/_apksigner"
  cp "$TC/_apksigner/libs/apksigner/apksigner.jar" "$TC/apksigner.jar"
  rm -rf "$TC/_apksigner"
fi

echo "==> Kotlin compiler 2.0.21 (npm registry, package kotlin-compiler)"
if [ ! -x "$TC/kotlinc/bin/kotlinc" ]; then
  curl -4 -sL --retry 3 -o /tmp/kc.tgz \
    "https://registry.npmjs.org/kotlin-compiler/-/kotlin-compiler-2.0.21.tgz"
  mkdir -p "$TC/_kc"
  tar xzf /tmp/kc.tgz -C "$TC/_kc"
  mv "$TC/_kc/package" "$TC/kotlinc"
  rm -rf "$TC/_kc" /tmp/kc.tgz
fi

echo "==> Toolchain ready:"
du -sh "$TC"
echo
echo "Build with:  python3 tools/build_apk.py"
echo "Run tests:   tools/run_tests.sh"
