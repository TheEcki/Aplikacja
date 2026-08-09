#!/usr/bin/env python3
"""
VitalTrack — offline APK build script (Kotlin, multi-module source layout).

Builds the Android app WITHOUT Gradle / Maven / Google servers, using a locally
assembled toolchain (JDK 8 + android.jar + aapt2 + dx + apksigner + kotlinc).

Source layout (mirrors the Gradle module structure):
    app/src/main/...            manifest, res, assets, app module sources
    core/*/src/main/java/...    core modules
    device/*/src/main/java/...  device modules
    feature/*/src/main/java/... feature modules
    app/src/main/java-demo/...  DEMO-ONLY sources (--demo flag)

Usage:
    python3 tools/build_apk.py [--out dist/VitalTrack.apk] [--release] [--demo]

    --demo  includes java-demo (DemoDataProvider) so the UI can be developed
            without a watch. The default build NEVER contains demo data.
"""
import os, shutil, subprocess, sys, zipfile, tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src", "main")
TC = os.environ.get("BIPSYNC_TOOLCHAIN", os.path.join(ROOT, "toolchain"))


def env_or(*names, default=None):
    for n in names:
        v = os.environ.get(n)
        if v:
            return v
    return default


def find_toolchain():
    jdk = env_or("BIPSYNC_JDK", default=os.path.join(TC, "jdk", "bin"))
    if os.path.isdir(os.path.join(jdk, "bin")):
        jdk = os.path.join(jdk, "bin")
    aapt2 = env_or("BIPSYNC_AAPT2", default=os.path.join(TC, "aapt2", "bin", "Linux", "aapt2"))
    dx = env_or("BIPSYNC_DX", default=os.path.join(TC, "dx.jar"))
    apksigner = env_or("BIPSYNC_APKSIGNER", default=os.path.join(TC, "apksigner.jar"))
    android_jar = env_or("BIPSYNC_ANDROID_JAR", default=os.path.join(TC, "android.jar"))
    link_jar = env_or("BIPSYNC_LINK_JAR", default=os.path.join(TC, "android-link.jar"))
    kotlinc = env_or("BIPSYNC_KOTLINC", default=os.path.join(TC, "kotlinc", "bin", "kotlinc"))
    stdlib = env_or("BIPSYNC_KOTLIN_STDLIB",
                    default=os.path.join(TC, "kotlinc", "lib", "kotlin-stdlib.jar"))
    for p, name in [(jdk, "JDK"), (aapt2, "aapt2"), (dx, "dx"), (apksigner, "apksigner"),
                    (android_jar, "android.jar"), (link_jar, "android-link.jar"),
                    (kotlinc, "kotlinc"), (stdlib, "kotlin-stdlib.jar")]:
        if not os.path.exists(p):
            print(f"[build] MISSING {name}: {p}")
            print("[build] Run tools/setup_toolchain.sh first, or set BIPSYNC_* env vars.")
            sys.exit(2)
    return jdk, aapt2, dx, apksigner, android_jar, link_jar, kotlinc, stdlib


def run(cmd, **kw):
    print("[build]", " ".join(str(c) for c in cmd))
    return subprocess.run(cmd, check=True, **kw)


def source_roots():
    """All module source roots + demo root."""
    roots = [os.path.join(APP, "java")]
    for group in ("core", "device", "feature"):
        d = os.path.join(ROOT, group)
        if os.path.isdir(d):
            for mod in sorted(os.listdir(d)):
                roots.append(os.path.join(d, mod, "src", "main", "java"))
    demo = os.path.join(APP, "java-demo")
    return roots, demo


def collect_sources(demo):
    roots, demo_root = source_roots()
    sources = []
    for r in roots:
        if os.path.isdir(r):
            for dp, dn, fn in os.walk(r):
                for f in fn:
                    if f.endswith(".kt"):
                        sources.append(os.path.join(dp, f))
    if demo:
        if os.path.isdir(demo_root):
            for dp, dn, fn in os.walk(demo_root):
                for f in fn:
                    if f.endswith(".kt"):
                        sources.append(os.path.join(dp, f))
            print(f"[build] demo variant: including {demo_root}")
        else:
            print("[build] WARNING: --demo requested but java-demo not found")
    return sorted(set(sources))


def aapt2_compile(aapt2, res_dir, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    run([aapt2, "compile", "--dir", res_dir, "-o", out_dir])


def aapt2_link(aapt2, link_jar, manifest, flat_dir, out_apk, java_dir, assets_dir=None):
    flats = [os.path.join(flat_dir, f) for f in sorted(os.listdir(flat_dir)) if f.endswith(".flat")]
    cmd = [aapt2, "link", "-o", out_apk, "--manifest", manifest, "-I", link_jar,
           "--java", java_dir, "--auto-add-overlay"]
    if assets_dir and os.path.isdir(assets_dir):
        cmd += ["-A", assets_dir]
    cmd += flats
    run(cmd)


def compile_sources(jdk, kotlinc, android_jar, stdlib_path, java_dir, sources_kt, class_dir):
    """Compile the aapt2-generated R.java with javac, then all .kt with kotlinc."""
    os.makedirs(class_dir, exist_ok=True)
    gen_java = []
    for dp, dn, fn in os.walk(java_dir):
        for f in fn:
            if f.endswith(".java"):
                gen_java.append(os.path.join(dp, f))
    if gen_java:
        with open(os.path.join(java_dir, "sources_java.txt"), "w") as f:
            f.write("\n".join(sorted(gen_java)))
        run([os.path.join(jdk, "javac"), "-source", "8", "-target", "8",
             "-encoding", "UTF-8", "-nowarn",
             "-classpath", android_jar + os.pathsep + java_dir,
             "-sourcepath", java_dir,
             "-d", class_dir, "@" + os.path.join(java_dir, "sources_java.txt")])
    if sources_kt:
        with open(os.path.join(java_dir, "sources_kt.txt"), "w") as f:
            f.write("\n".join(sources_kt))
        env = dict(os.environ)
        jdk_home = os.path.dirname(os.path.dirname(jdk))
        env["JAVA_HOME"] = jdk_home
        env["PATH"] = os.path.join(jdk, "") + os.pathsep + env.get("PATH", "")
        # -no-stdlib: kotlin-stdlib is dexed into the APK together with the
        # app classes, so add it explicitly to the compile classpath.
        cp = os.pathsep.join([class_dir, android_jar, java_dir, stdlib_path])
        cmd = [kotlinc, "-jvm-target", "1.8", "-nowarn", "-no-stdlib",
               "-classpath", cp,
               "-d", class_dir, "@" + os.path.join(java_dir, "sources_kt.txt")]
        run(cmd, env=env)


def dx(java, dx_jar, class_dirs, stdlib, dex_dir, min_api="28"):
    os.makedirs(dex_dir, exist_ok=True)
    dex_out = os.path.join(dex_dir, "classes.dex")
    # kotlin-stdlib 2.x is a multi-release JAR; the AOSP dx parser cannot read
    # META-INF/versions/*/module-info.class. Strip those entries.
    clean_stdlib = os.path.join(dex_dir, "kotlin-stdlib-clean.jar")
    with zipfile.ZipFile(stdlib, "r") as zin, zipfile.ZipFile(clean_stdlib, "w") as zout:
        for item in zin.infolist():
            if item.filename.startswith("META-INF/versions/"):
                continue
            if item.filename == "META-INF/MANIFEST.MF":
                continue
            zout.writestr(item, zin.read(item.filename))
    args = [java, "-cp", dx_jar, "com.android.dx.command.Main", "--dex",
            "--min-sdk-version=" + min_api, "--output=" + dex_out]
    args += class_dirs
    args += [clean_stdlib]
    run(args)


def add_dex_to_apk(apk, dex_dir):
    dex = os.path.join(dex_dir, "classes.dex")
    tmp = apk + ".tmp"
    with zipfile.ZipFile(apk, "r") as zin, zipfile.ZipFile(tmp, "w") as zout:
        for item in zin.infolist():
            zout.writestr(item, zin.read(item.filename))
        zout.write(dex, "classes.dex")
    shutil.move(tmp, apk)


def rebuild_zip(apk, out, align):
    """Re-write the APK so every uncompressed entry starts at a 4-byte aligned
    offset (equivalent of zipalign -p 4)."""
    import struct, zlib
    with zipfile.ZipFile(apk, "r") as z:
        items = z.infolist()
        items.sort(key=lambda i: i.filename)
        payload = bytearray()
        offsets = {}
        for it in items:
            data = z.read(it.filename)
            name = it.filename.encode("utf-8")
            crc = zlib.crc32(data) & 0xffffffff
            local_len = 30 + len(name)
            if it.compress_type == zipfile.ZIP_STORED:
                if (len(payload) + local_len) % align != 0:
                    payload += b"\x00" * (align - (len(payload) + local_len) % align)
                local = struct.pack("<IHHHHHIIIHH", 0x04034b50, 20, 0,
                                    0, 0, 0, crc, len(data), len(data), len(name), 0)
                offsets[it.filename] = (len(payload), it, data, len(data), crc)
                payload += local + name + data
            else:
                co = zlib.compressobj(9, zlib.DEFLATED, -15)
                cdata = co.compress(data) + co.flush()
                local = struct.pack("<IHHHHHIIIHH", 0x04034b50, 20, 0,
                                    8, 0, 0, crc, len(cdata), len(data), len(name), 0)
                offsets[it.filename] = (len(payload), it, cdata, len(data), crc)
                payload += local + name + cdata
        cd = bytearray()
        for fname, (off, it, stored, orig_len, crc) in offsets.items():
            name = fname.encode("utf-8")
            comp = 0 if it.compress_type == zipfile.ZIP_STORED else 8
            ext_attr = (it.external_attr >> 16) & 0xffff
            cd += struct.pack("<IHHHHHHIIIHHHHHII", 0x02014b50, 20, 20,
                              0, comp, 0, 0, crc, len(stored), orig_len, len(name),
                              0, 0, 0, 0, ext_attr, off)
            cd += name
        eocd = struct.pack("<IHHHHIIH", 0x06054b50, 0, 0, len(offsets), len(offsets),
                           len(cd), len(payload), 0)
        with open(out, "wb") as f:
            f.write(bytes(payload))
            f.write(bytes(cd))
            f.write(eocd)


def sign(java, apksigner, keystore, kpass, apk_in, apk_out):
    if not os.path.exists(keystore):
        ks_dir = os.path.dirname(keystore)
        os.makedirs(ks_dir, exist_ok=True)
        jdk = os.path.dirname(os.path.dirname(java))
        run([os.path.join(jdk, "bin", "keytool"), "-genkeypair", "-keystore", keystore,
             "-alias", "pulsetrack", "-keyalg", "RSA", "-keysize", "2048",
             "-validity", "10000", "-storepass", kpass, "-keypass", kpass,
             "-dname", "CN=VitalTrack, OU=Dev, O=VitalTrack, L=Gdansk, C=PL"])
    run([java, "-jar", apksigner, "sign", "--ks", keystore, "--ks-pass", "pass:" + kpass,
         "--ks-key-alias", "vitaltrack", "--key-pass", "pass:" + kpass,
         "--out", apk_out, apk_in])


def main():
    release = "--release" in sys.argv
    demo = "--demo" in sys.argv
    target_sdk = 34
    if "--target-sdk" in sys.argv:
        target_sdk = int(sys.argv[sys.argv.index("--target-sdk") + 1])
    print(f"[build] targetSdk = {target_sdk} (minSdk 28)")
    out = None
    if "--out" in sys.argv:
        out = sys.argv[sys.argv.index("--out") + 1]
    if not out:
        out = os.path.join(ROOT, "dist", "VitalTrack.apk")

    jdk_bin, aapt2, dx_jar, apksigner, android_jar, link_jar, kotlinc, stdlib = find_toolchain()
    java = os.path.join(jdk_bin, "java")

    build = tempfile.mkdtemp(prefix="vitaltrack_build_")
    res_flat = os.path.join(build, "flat")
    java_dir = os.path.join(build, "gen")
    class_dir = os.path.join(build, "classes")
    dex_dir = os.path.join(build, "dex")
    base_apk = os.path.join(build, "base.apk")
    aligned_apk = os.path.join(build, "aligned.apk")
    ks = env_or("BIPSYNC_KEYSTORE", default=os.path.join(ROOT, "keystore", "vitaltrack-release.keystore"))
    kpass = env_or("BIPSYNC_KEYSTORE_PASS", default="vitaltrack123")

    manifest = os.path.join(APP, "AndroidManifest.xml")
    if target_sdk != 36:
        import re
        manifest = os.path.join(build, "AndroidManifest.xml")
        with open(os.path.join(APP, "AndroidManifest.xml"), "r") as f:
            src = f.read()
        src = re.sub(r'android:targetSdkVersion="\d+"',
                     f'android:targetSdkVersion="{target_sdk}"', src)
        with open(manifest, "w") as f:
            f.write(src)
    res = os.path.join(APP, "res")

    print("[build] 1/6 resources (aapt2 compile)")
    aapt2_compile(aapt2, res, res_flat)
    print("[build] 2/6 link (aapt2 link)")
    os.makedirs(java_dir, exist_ok=True)
    assets_dir = os.path.join(APP, "assets")
    aapt2_link(aapt2, link_jar, manifest, res_flat, base_apk, java_dir, assets_dir)

    print("[build] 3/6 compile (javac R + kotlinc)")
    sources_kt = collect_sources(demo)
    compile_sources(jdk_bin, kotlinc, android_jar, stdlib, java_dir, sources_kt, class_dir)

    print("[build] 4/6 dex (dx, minSdk 28)")
    dx(java, dx_jar, [class_dir], stdlib, dex_dir, min_api="28")

    print("[build] 5/6 package + align")
    add_dex_to_apk(base_apk, dex_dir)
    rebuild_zip(base_apk, aligned_apk, 4)

    print("[build] 6/6 sign")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sign(java, apksigner, ks, kpass, aligned_apk, out)

    print("[build] verify")
    run([java, "-jar", apksigner, "verify", "--print-certs", out])
    print("[build] DONE ->", os.path.abspath(out))
    shutil.rmtree(build, ignore_errors=True)


if __name__ == "__main__":
    main()
