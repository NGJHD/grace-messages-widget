"""Batch-transcribe every sermon in data/inventory.json with whisper.cpp.

Resumable: sermons that already have data/transcripts/<id>.json are skipped.
Each sermon is decoded (track folders joined) to a temporary 16 kHz mono WAV,
transcribed, and the WAV deleted afterwards.

Usage:
  python pipeline/transcribe.py --whisper tools/whisper-cuda/Release/whisper-cli.exe --gpu 1
  python pipeline/transcribe.py --limit 3          # just a few, e.g. to test
"""
import argparse
import json
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
INVENTORY = ROOT / "data" / "inventory.json"
AUDIO = ROOT / "data" / "audio"
TRANSCRIPTS = ROOT / "data" / "transcripts"


def readable(src: str) -> bool:
    """False for stub/corrupt parts (e.g. 3 KB empty CD tracks) that would abort the concat."""
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                        "-of", "csv=p=0", src], capture_output=True, text=True)
    try:
        return r.returncode == 0 and float(r.stdout.strip()) > 1.0
    except ValueError:
        return False


def to_wav(sources: list[str], out: Path) -> None:
    good = [s for s in sources if readable(s)]
    for s in sources:
        if s not in good:
            print(f"    skipping unreadable part: {Path(s).name}", flush=True)
    if not good:
        raise RuntimeError("no readable audio parts")
    sources = good
    cmd = ["ffmpeg", "-v", "error", "-y"]
    for s in sources:
        cmd += ["-i", s]
    if len(sources) > 1:
        streams = "".join(f"[{i}:a:0]" for i in range(len(sources)))
        cmd += ["-filter_complex", f"{streams}concat=n={len(sources)}:v=0:a=1[a]", "-map", "[a]"]
    else:
        cmd += ["-map", "0:a:0"]
    cmd += ["-ar", "16000", "-ac", "1", "-c:a", "pcm_s16le", str(out)]
    subprocess.run(cmd, check=True)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--whisper", default=str(ROOT / "tools" / "whisper-cuda" / "Release" / "whisper-cli.exe"))
    ap.add_argument("--model", default=r"C:\llama\models\ggml-large-v3-turbo.bin")
    ap.add_argument("--vad-model", default=r"C:\llama\models\ggml-silero-v5.1.2.bin")
    ap.add_argument("--gpu", default=None, help="CUDA device index to use (sets CUDA_VISIBLE_DEVICES)")
    ap.add_argument("--threads", type=int, default=8)
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--shard", default="0/1", help="i/n: process every n-th sermon starting at i (one per GPU)")
    args = ap.parse_args()

    env = dict(os.environ)
    if args.gpu is not None:
        env["CUDA_VISIBLE_DEVICES"] = str(args.gpu)

    AUDIO.mkdir(parents=True, exist_ok=True)
    TRANSCRIPTS.mkdir(parents=True, exist_ok=True)
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    todo = [s for s in inventory if not (TRANSCRIPTS / f"{s['id']}.json").exists()]
    i, n = (int(x) for x in args.shard.split("/"))
    todo = todo[i::n]
    if args.limit:
        todo = todo[: args.limit]
    total_audio = sum(s["duration"] for s in todo)
    print(f"{len(todo)} to transcribe ({total_audio / 3600:.1f} h of audio)", flush=True)

    done_audio = 0.0
    t0 = time.time()
    for n, s in enumerate(todo, 1):
        wav = AUDIO / f"{s['id']}.{i}.wav"
        out = TRANSCRIPTS / s["id"]
        start = time.time()
        try:
            to_wav(s["sources"], wav)
            subprocess.run(
                [args.whisper, "-m", args.model, "--vad", "--vad-model", args.vad_model,
                 "-f", str(wav), "-t", str(args.threads), "-l", "en",
                 "-otxt", "-oj", "-of", str(out), "-np"],
                check=True, env=env, stdout=subprocess.DEVNULL,
            )
        except subprocess.CalledProcessError as e:
            print(f"[{n}/{len(todo)}] FAILED {s['id']}: {e}", flush=True)
            continue
        finally:
            wav.unlink(missing_ok=True)
        done_audio += s["duration"]
        took = time.time() - start
        speed = done_audio / (time.time() - t0)
        eta = (total_audio - done_audio) / speed / 60 if speed else 0
        print(f"[{n}/{len(todo)}] {s['id']}  {s['duration'] / 60:.0f} min audio in {took:.0f}s"
              f"  ({speed:.0f}x realtime, ~{eta:.0f} min left)", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
