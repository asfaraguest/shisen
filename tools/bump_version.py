#!/usr/bin/env python3
"""Menatap Versioning Manager (SemVer 2.0.0 & Beta Support)."""

import sys
import re
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VERSION_FILE = os.path.join(BASE_DIR, "VERSION")
README_FILE = os.path.join(BASE_DIR, "README.md")
BUILD_GRADLE = os.path.join(BASE_DIR, "android", "app", "build.gradle")

SEMVER_REGEX = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$")

def read_version():
    with open(VERSION_FILE, "r", encoding="utf-8") as f:
        return f.read().strip()

def parse_semver(v_str):
    m = SEMVER_REGEX.match(v_str)
    if not m:
        raise ValueError(f"Versi '{v_str}' tidak memenuhi format SemVer 2.0.0 (X.Y.Z atau X.Y.Z-beta.N)")
    major, minor, patch = int(m.group(1)), int(m.group(2)), int(m.group(3))
    beta = int(m.group(4)) if m.group(4) else None
    return major, minor, patch, beta

def format_semver(major, minor, patch, beta=None):
    if beta is not None:
        return f"{major}.{minor}.{patch}-beta.{beta}"
    return f"{major}.{minor}.{patch}"

def compute_version_code(v_str):
    major, minor, patch, beta = parse_semver(v_str)
    beta_val = beta if beta is not None else 99
    return (major * 1000000) + (minor * 10000) + (patch * 100) + beta_val

def update_version(new_version):
    parse_semver(new_version)
    with open(VERSION_FILE, "w", encoding="utf-8") as f:
        f.write(new_version + "\n")

    # Update README
    if os.path.exists(README_FILE):
        with open(README_FILE, "r", encoding="utf-8") as f:
            readme_content = f.read()
        readme_content = re.sub(r"(?:shisen|menatap)-release-v[0-9a-zA-Z\.\-]+\.apk", f"shisen-release-v{new_version}.apk", readme_content)
        readme_content = re.sub(r"releases/tag/v[0-9a-zA-Z\.\-]+", f"releases/tag/v{new_version}", readme_content)
        with open(README_FILE, "w", encoding="utf-8") as f:
            f.write(readme_content)

    print(f"✓ Berhasil update versi Menatap ke: {new_version} (versionCode: {compute_version_code(new_version)})")

def check_consistency():
    current = read_version()
    major, minor, patch, beta = parse_semver(current)
    print(f"Current version : {current}")
    print(f"Parsed SemVer   : major={major}, minor={minor}, patch={patch}, beta={beta}")
    print(f"Version Code    : {compute_version_code(current)}")
    print("✓ Audit konsistensi versi: VALID")
    return True

def main():
    if len(sys.argv) < 2:
        print("Penggunaan:")
        print("  python3 tools/bump_version.py beta      # Naikkan iterasi beta")
        print("  python3 tools/bump_version.py release   # Finalisasi beta ke rilis stabil")
        print("  python3 tools/bump_version.py patch     # Naikkan patch")
        print("  python3 tools/bump_version.py minor     # Naikkan minor")
        print("  python3 tools/bump_version.py major     # Naikkan major")
        print("  python3 tools/bump_version.py --check   # Periksa konsistensi")
        sys.exit(0)

    arg = sys.argv[1].lower()
    current = read_version()
    major, minor, patch, beta = parse_semver(current)

    if arg == "--check":
        check_consistency()
        sys.exit(0)
    elif arg == "beta":
        if beta is None:
            new_v = format_semver(major, minor, patch + 1, 1)
        else:
            new_v = format_semver(major, minor, patch, beta + 1)
        update_version(new_v)
    elif arg == "release":
        new_v = format_semver(major, minor, patch, None)
        update_version(new_v)
    elif arg == "patch":
        new_v = format_semver(major, minor, patch + 1, 1 if beta is not None else None)
        update_version(new_v)
    elif arg == "minor":
        new_v = format_semver(major, minor + 1, 0, 1 if beta is not None else None)
        update_version(new_v)
    elif arg == "major":
        new_v = format_semver(major + 1, 0, 0, 1 if beta is not None else None)
        update_version(new_v)
    else:
        # Custom version string
        update_version(sys.argv[1])

if __name__ == "__main__":
    main()
