#!/usr/bin/env python3
"""
Export a Figma node as PNG at 5 Android densities.

Usage:
    python3 export.py <token> <file_key> <node_id> <filename> <res_path>

    node_id format: "1427:6150"
    filename: snake_case, no extension (e.g. bg_more_has_carrier)
    res_path: absolute path to app/src/main/res
"""

import subprocess
import json
import sys
import os

def main():
    if len(sys.argv) != 6:
        print("Usage: export.py <token> <file_key> <node_id> <filename> <res_path>")
        sys.exit(1)

    token, file_key, node_id, base, res = sys.argv[1:]

    scales = [
        ("drawable-mdpi",    "1"),
        ("drawable-hdpi",    "1.5"),
        ("drawable-xhdpi",   "2"),
        ("drawable-xxhdpi",  "3"),
        ("drawable-xxxhdpi", "4"),
    ]

    results = []

    for folder, scale in scales:
        result = subprocess.run([
            "curl", "-s",
            f"https://api.figma.com/v1/images/{file_key}?ids={node_id}&scale={scale}&format=png",
            "-H", f"X-Figma-Token: {token}"
        ], capture_output=True, text=True)

        data = json.loads(result.stdout)

        if "err" in data and data["err"]:
            print(f"ERROR [{folder}]: {data['err']}")
            sys.exit(1)

        url = list(data["images"].values())[0]
        dest = os.path.join(res, folder, f"{base}.png")
        subprocess.run(["curl", "-s", "-o", dest, url], check=True)

        size_kb = os.path.getsize(dest) // 1024
        results.append((folder, f"{base}.png", f"{size_kb}K"))
        print(f"OK  {folder}/{base}.png  ({size_kb}K)")

    print("\n--- Summary ---")
    for folder, fname, size in results:
        print(f"  {folder}/{fname}  {size}")

if __name__ == "__main__":
    main()
