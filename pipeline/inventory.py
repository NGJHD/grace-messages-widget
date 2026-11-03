"""Build a de-duplicated inventory of sermons from the source folder.

Rules
- Each top-level media file is a candidate; files with the same name but different
  extensions are one sermon (prefer audio-only: mp3 > m4a > mka > mp4 > mkv).
- Each folder of tracks is one sermon (tracks joined in name order). Nested
  folders inside a sermon folder (e.g. "Original") are ignored.
- A folder and a single file with the same date + matching title are the same
  sermon; the single file wins.
- Titles come from the file name, or for track folders (whose names are cut short)
  from the audio's "album" tag when available.

Output: data/inventory.json
Usage:  python pipeline/inventory.py [SOURCE_DIR] [OUT_JSON]   (OUT_JSON for a dry run)

Ids are stable across re-scans (date + slug of the title), so existing transcripts and
messages keep matching; only newly added sermons get new ids.
"""
import json
import re
import zlib
import subprocess
import sys
from pathlib import Path

SOURCE = Path(sys.argv[1] if len(sys.argv) > 1 else r"T:\Y\Sermons")
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(__file__).resolve().parent.parent / "data" / "inventory.json"
MEDIA = {".mp3": 0, ".m4a": 1, ".mka": 2, ".wav": 3, ".mp4": 4, ".mkv": 5}
DATE_RE = re.compile(r"^(\d{4}-\d{2}-\d{2})(?:\s+(.*))?$")


def norm(s: str) -> str:
    s = s.replace("\u2019", "'").replace("\u2014", " ").replace("_", " ")
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def slug(s: str) -> str:
    s = s.replace("\u2019", "").replace("'", "")
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")[:60]


def probe(path: Path) -> dict:
    r = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration:format_tags=album,title",
         "-of", "json", str(path)],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    try:
        fmt = json.loads(r.stdout).get("format", {})
    except json.JSONDecodeError:
        return {"duration": 0.0, "tags": {}}
    tags = {k.lower(): v for k, v in (fmt.get("tags") or {}).items()}
    return {"duration": float(fmt.get("duration") or 0), "tags": tags}


def split_name(name: str):
    m = DATE_RE.match(name)
    return (m.group(1), (m.group(2) or "").strip()) if m else (None, name.strip())


def main():
    singles: dict[str, list[Path]] = {}
    for f in SOURCE.iterdir():
        if f.is_file() and f.suffix.lower() in MEDIA:
            singles.setdefault(f.stem, []).append(f)

    items = []
    for stem, files in singles.items():
        best = min(files, key=lambda p: MEDIA[p.suffix.lower()])
        date, title = split_name(stem)
        title = title or "Untitled"  # e.g. a bare-date recording; renamed later from its content
        items.append({"date": date, "title": title, "kind": "file", "sources": [str(best)],
                      "alternates": [str(p) for p in files if p != best]})

    for d in SOURCE.iterdir():
        if not d.is_dir():
            continue
        tracks = sorted(p for p in d.iterdir() if p.is_file() and p.suffix.lower() in MEDIA)
        if not tracks:
            print(f"skip (no tracks): {d.name}")
            continue
        date, title = split_name(d.name)
        _, album = split_name(probe(tracks[0])["tags"].get("album", "").strip())
        if album and norm(album).startswith(norm(title)[:15]):
            title = album
        items.append({"date": date, "title": title, "kind": "tracks",
                      "sources": [str(p) for p in tracks], "alternates": []})

    # Folder vs single-file duplicates: same date and title prefix -> keep the single file.
    files_by_date: dict[str, list[dict]] = {}
    for it in items:
        if it["kind"] == "file":
            files_by_date.setdefault(it["date"] or "", []).append(it)
    kept = []
    for it in items:
        if it["kind"] == "tracks":
            twin = next((f for f in files_by_date.get(it["date"] or "", [])
                         if norm(f["title"])[:15] == norm(it["title"])[:15]), None)
            if twin:
                print(f"duplicate: folder '{it['title']}' == file '{twin['title']}'")
                twin["alternates"] += it["sources"]
                continue
        kept.append(it)

    for it in kept:
        it["duration"] = round(sum(probe(Path(s))["duration"] for s in it["sources"]), 1)
        it["id"] = f"{it['date'] or 'undated'}-{slug(it['title'])}"
    kept.sort(key=lambda x: (x["date"] or "9999", x["title"]))

    ids = [k["id"] for k in kept]
    dupes = {i for i in ids if ids.count(i) > 1}
    for it in kept:
        if it["id"] in dupes:
            it["id"] += "-" + str(zlib.crc32(Path(it["sources"][0]).name.encode()) % 10000)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(kept, indent=1, ensure_ascii=False), encoding="utf-8")
    hours = sum(k["duration"] for k in kept) / 3600
    print(f"\n{len(kept)} sermons, {hours:.0f} hours of audio -> {OUT}")


if __name__ == "__main__":
    main()
