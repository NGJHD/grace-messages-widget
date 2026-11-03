"""Merge data/messages/<sermon-id>.json files into the app's bundled dataset.

Each messages file is a JSON list of:
  {"category": "inspirational"|"insight", "text": str, "explanation": str, "scripture"?: str}
Sermon titles/dates come from data/inventory.json (a messages file may override the
title with a top-level {"title": ..., "messages": [...]} object, e.g. for untitled recordings).

Output: android/app/src/main/assets/messages.json
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
INVENTORY = ROOT / "data" / "inventory.json"
MESSAGES = ROOT / "data" / "messages"
OUT = ROOT / "android" / "app" / "src" / "main" / "assets" / "messages.json"
MAX_TEXT = 500


def main() -> int:
    inventory = {s["id"]: s for s in json.loads(INVENTORY.read_text(encoding="utf-8"))}
    sermons, messages, errors = [], [], []

    for f in sorted(MESSAGES.glob("*.json")):
        sid = f.stem
        if sid not in inventory:
            errors.append(f"{f.name}: no sermon with id '{sid}' in inventory")
            continue
        raw = json.loads(f.read_text(encoding="utf-8"))
        title = inventory[sid]["title"]
        if isinstance(raw, dict):
            title = raw.get("title", title)
            raw = raw["messages"]
        sermons.append({"id": sid, "title": title, "date": inventory[sid]["date"]})
        for i, m in enumerate(raw, 1):
            where = f"{f.name} #{i}"
            if m.get("category") not in ("inspirational", "insight"):
                errors.append(f"{where}: bad category {m.get('category')!r}")
            if not m.get("text") or len(m["text"]) > MAX_TEXT:
                errors.append(f"{where}: text missing or longer than {MAX_TEXT}")
            if not m.get("explanation"):
                errors.append(f"{where}: explanation missing")
            entry = {"id": f"{sid}-{i}", "sermonId": sid, "category": m.get("category"),
                     "text": m.get("text", "").strip(), "explanation": m.get("explanation", "").strip()}
            if m.get("scripture"):
                entry["scripture"] = m["scripture"]
            messages.append(entry)

    if errors:
        print("\n".join(errors))
        return 1

    OUT.write_text(json.dumps({"version": 1, "sermons": sermons, "messages": messages},
                              ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    lengths = sorted(len(m["text"]) for m in messages)
    print(f"{len(messages)} messages from {len(sermons)} sermons -> {OUT}")
    for limit in (80, 120, 160, 200, 300):
        n = sum(1 for x in lengths if x <= limit)
        print(f"  <= {limit:3} chars: {n:4} ({n * 100 // max(len(lengths), 1)}%)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
