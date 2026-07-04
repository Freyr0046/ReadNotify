---
name: figma-to-avd
description: Export a Figma node as Android Vector Drawable (AVD) XML and save to res/drawable/. Trigger when the user provides a Figma URL and wants to convert a vector/icon to AVD, or says things like "幫我從 Figma 轉 AVD", "convert to vector drawable", "Figma 轉 xml", "figma link to svg", "存成 vector drawable"。
---

## Workflow

### 1. Gather inputs

**Figma URL** — must contain `node-id`. Parse:
- `fileKey`: path segment after `/design/`
- `nodeId`: `node-id` query param, convert `-` → `:` (e.g. `1053-2789` → `1053:2789`)

**Token** — resolve in this order:
1. Check env var: `echo $FIGMA_TOKEN`
2. Check `~/.zshrc` for `export FIGMA_TOKEN=...`
3. Only if both are empty, ask the user:
   > 請提供 Figma Personal Access Token（`figd_...`）。
   > 取得方式：figma.com/settings → Security → Personal access tokens → 權限只需勾選 **File content: Read-only**

**Filename** — ask if not provided:
> 請輸入 Android 檔名（snake_case，不含副檔名），例如 `ic_arrow_down`

### 2. Preview

Use `mcp__figma-remote-mcp__get_screenshot` with the parsed `fileKey` and `nodeId` to show a preview before converting.

### 3. Find res/drawable path

```bash
find . -type d -name "drawable" -path "*/main/res/*" | head -1
```

### 4. Export SVG & convert to AVD

Run the bundled script:
```bash
python3 <skill_dir>/scripts/export.py <token> <fileKey> <nodeId> <filename> <drawable_path>
```

The script will:
1. Call Figma REST API to get SVG export URL
2. Download the SVG
3. Parse and convert to Android Vector Drawable XML
4. Write `<filename>.xml` to the target drawable folder

### 5. Confirm

Show the output path and a preview of the generated XML.

### Notes

- This skill handles **simple vector shapes** (paths, fills, strokes, groups).
- If the node contains raster effects (blur, image fills), Figma will export a raster fallback — the script will warn and abort. Use `figma-to-android-png` instead.
- Colors are written as hex. If the project uses `@color/` references, remind the user to replace them manually.
