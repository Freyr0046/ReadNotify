#!/usr/bin/env python3
"""
Export a Figma node as Android Vector Drawable (AVD) XML.

Usage:
    python3 export.py <token> <file_key> <node_id> <filename> <drawable_path>

    node_id format:  "1053:2789"
    filename:        snake_case, no extension (e.g. ic_arrow_down)
    drawable_path:   absolute path to res/drawable
"""

import json
import os
import subprocess
import sys
import xml.etree.ElementTree as ET


def figma_export_svg(token, file_key, node_id):
    result = subprocess.run([
        "curl", "-s",
        f"https://api.figma.com/v1/images/{file_key}?ids={node_id}&format=svg",
        "-H", f"X-Figma-Token: {token}"
    ], capture_output=True, text=True)
    data = json.loads(result.stdout)
    if data.get("err"):
        print(f"ERROR: Figma API returned error: {data['err']}")
        sys.exit(1)
    url = list(data["images"].values())[0]
    if not url:
        print("ERROR: Figma returned no image URL. The node may contain raster effects.")
        print("       Use figma-to-android-png skill instead.")
        sys.exit(1)
    return url


def download_svg(url):
    result = subprocess.run(["curl", "-s", url], capture_output=True, text=True)
    content = result.stdout.strip()
    if not content.startswith("<svg"):
        print("ERROR: Downloaded content is not SVG (may be raster). Use figma-to-android-png instead.")
        sys.exit(1)
    return content


def parse_color(color):
    """Normalize color: 'none' → keep as-is, else ensure #RRGGBB format."""
    if not color or color == "none":
        return None
    if color.startswith("#"):
        return color.upper()
    return color


def parse_transform(transform_str):
    """Extract translate values from SVG transform attribute (basic support)."""
    # Only handles translate(x,y) or translate(x y)
    if not transform_str:
        return None
    import re
    m = re.match(r'translate\(([-\d.]+)[,\s]+([-\d.]+)\)', transform_str)
    if m:
        return float(m.group(1)), float(m.group(2))
    return None


def svg_to_avd(svg_content):
    ET.register_namespace("", "http://www.w3.org/2000/svg")
    root = ET.fromstring(svg_content)

    width = root.get("width", "24")
    height = root.get("height", "24")
    viewbox = root.get("viewBox", f"0 0 {width} {height}")
    vb_parts = viewbox.strip().replace(",", " ").split()
    viewport_w = vb_parts[2] if len(vb_parts) >= 4 else width
    viewport_h = vb_parts[3] if len(vb_parts) >= 4 else height

    # Strip units (px, pt, etc.)
    def strip_unit(val):
        return val.replace("px", "").replace("pt", "").strip()

    width = strip_unit(width)
    height = strip_unit(height)

    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{width}dp"',
        f'    android:height="{height}dp"',
        f'    android:viewportWidth="{viewport_w}"',
        f'    android:viewportHeight="{viewport_h}">',
    ]

    ns = {"svg": "http://www.w3.org/2000/svg"}

    def process_element(elem, indent=1):
        pad = "    " * indent
        tag = elem.tag.split("}")[-1] if "}" in elem.tag else elem.tag

        if tag == "path":
            d = elem.get("d", "")
            fill = parse_color(elem.get("fill", "#000000"))
            stroke = parse_color(elem.get("stroke"))
            stroke_width = elem.get("stroke-width")
            fill_rule = elem.get("fill-rule")
            opacity = elem.get("opacity")
            fill_opacity = elem.get("fill-opacity")

            attrs = [f'android:pathData="{d}"']
            if fill:
                attrs.append(f'android:fillColor="{fill}"')
            else:
                attrs.append('android:fillColor="@android:color/transparent"')
            if stroke:
                attrs.append(f'android:strokeColor="{stroke}"')
            if stroke_width:
                attrs.append(f'android:strokeWidth="{stroke_width}"')
            if fill_rule == "evenodd":
                attrs.append('android:fillType="evenOdd"')
            if opacity:
                attrs.append(f'android:fillAlpha="{opacity}"')
            elif fill_opacity and fill_opacity != "1":
                attrs.append(f'android:fillAlpha="{fill_opacity}"')

            attr_str = "\n".join(f'        {"    " * (indent - 1)}{a}' for a in attrs)
            lines.append(f'{pad}<path')
            lines.append(f'{attr_str}/>')

        elif tag == "g":
            group_attrs = []
            transform = elem.get("transform")
            opacity = elem.get("opacity")
            if opacity:
                group_attrs.append(f'android:alpha="{opacity}"')
            if transform:
                t = parse_transform(transform)
                if t:
                    group_attrs.append(f'android:translateX="{t[0]}"')
                    group_attrs.append(f'android:translateY="{t[1]}"')

            if group_attrs:
                attr_str = " ".join(group_attrs)
                lines.append(f'{pad}<group {attr_str}>')
            else:
                lines.append(f'{pad}<group>')
            for child in elem:
                process_element(child, indent + 1)
            lines.append(f'{pad}</group>')

        elif tag == "circle":
            cx = elem.get("cx", "0")
            cy = elem.get("cy", "0")
            r = elem.get("r", "0")
            fill = parse_color(elem.get("fill", "#000000"))
            # Convert circle to path
            d = (f"M {cx},{float(cy)-float(r)} "
                 f"A {r},{r} 0 1,0 {cx},{float(cy)+float(r)} "
                 f"A {r},{r} 0 1,0 {cx},{float(cy)-float(r)} Z")
            attrs = [f'android:pathData="{d}"']
            if fill:
                attrs.append(f'android:fillColor="{fill}"')
            attr_str = "\n".join(f'        {"    " * (indent - 1)}{a}' for a in attrs)
            lines.append(f'{pad}<path')
            lines.append(f'{attr_str}/>')

        elif tag == "rect":
            x = elem.get("x", "0")
            y = elem.get("y", "0")
            w = elem.get("width", "0")
            h = elem.get("height", "0")
            rx = elem.get("rx", "0")
            fill = parse_color(elem.get("fill", "#000000"))
            stroke = parse_color(elem.get("stroke"))
            stroke_width = elem.get("stroke-width")

            if rx and rx != "0":
                d = (f"M {float(x)+float(rx)},{y} "
                     f"H {float(x)+float(w)-float(rx)} "
                     f"Q {float(x)+float(w)},{y} {float(x)+float(w)},{float(y)+float(rx)} "
                     f"V {float(y)+float(h)-float(rx)} "
                     f"Q {float(x)+float(w)},{float(y)+float(h)} {float(x)+float(w)-float(rx)},{float(y)+float(h)} "
                     f"H {float(x)+float(rx)} "
                     f"Q {x},{float(y)+float(h)} {x},{float(y)+float(h)-float(rx)} "
                     f"V {float(y)+float(rx)} "
                     f"Q {x},{y} {float(x)+float(rx)},{y} Z")
            else:
                d = f"M {x},{y} H {float(x)+float(w)} V {float(y)+float(h)} H {x} Z"

            attrs = [f'android:pathData="{d}"']
            if fill:
                attrs.append(f'android:fillColor="{fill}"')
            if stroke:
                attrs.append(f'android:strokeColor="{stroke}"')
            if stroke_width:
                attrs.append(f'android:strokeWidth="{stroke_width}"')
            attr_str = "\n".join(f'        {"    " * (indent - 1)}{a}' for a in attrs)
            lines.append(f'{pad}<path')
            lines.append(f'{attr_str}/>')

        # Skip defs, title, desc, etc.

    for child in root:
        process_element(child)

    lines.append("</vector>")
    return "\n".join(lines)


def main():
    if len(sys.argv) != 6:
        print("Usage: export.py <token> <file_key> <node_id> <filename> <drawable_path>")
        sys.exit(1)

    token, file_key, node_id, filename, drawable_path = sys.argv[1:]

    print(f"Fetching SVG from Figma: {file_key} / {node_id} ...")
    svg_url = figma_export_svg(token, file_key, node_id)

    print(f"Downloading SVG ...")
    svg_content = download_svg(svg_url)

    print("Converting SVG → AVD ...")
    avd_xml = svg_to_avd(svg_content)

    dest = os.path.join(drawable_path, f"{filename}.xml")
    with open(dest, "w", encoding="utf-8") as f:
        f.write(avd_xml)

    print(f"\nOK  {dest}")
    print("\n--- Generated XML ---")
    print(avd_xml)


if __name__ == "__main__":
    main()
