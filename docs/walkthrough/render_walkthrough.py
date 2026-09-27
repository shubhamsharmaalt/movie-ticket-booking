#!/usr/bin/env python3
"""Render the local code walkthrough. Requires macOS say, FFmpeg, and Pillow.

Usage: python render_walkthrough.py prepare|narrate|preview|build
No network requests or application/runtime dependencies are added.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import subprocess
import textwrap
import wave
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
WORK = ROOT / "target" / "walkthrough-build"
DATA = json.loads((HERE / "scenes.json").read_text())
W, H, FPS, RATE = 1920, 1080, 30, 48000
BG = "#0C1321"
PANEL = "#121E30"
TEXT = "#F2F6FD"
MUTED = "#A2B3C9"
TEAL = "#63DECC"
BLUE = "#84B5FF"
MONO = "/System/Library/Fonts/Menlo.ttc"
SANS = "/System/Library/Fonts/Avenir Next.ttc"


def run(args):
    subprocess.run([str(x) for x in args], check=True, stdout=subprocess.DEVNULL)


def font(size, mono=False):
    return ImageFont.truetype(MONO if mono else SANS, size)


def segments():
    result = []
    for chapter, scene in enumerate(DATA["scenes"]):
        for step, item in enumerate(scene["segments"]):
            result.append({**item, "chapter": chapter, "step": step, "number": len(result) + 1})
    return result


def paths(item):
    stem = f"{item['number']:02d}"
    return {key: WORK / f"{stem}.{ext}" for key, ext in
            (("text", "txt"), ("aiff", "aiff"), ("wav", "wav"), ("hash", "sha256"), ("frame", "png"))}


def speech_text(text):
    # Keep on-screen names exact; give the voice natural word boundaries.
    names = ["MovieTicketsApplication", "SecurityConfiguration", "BookingController", "CatalogController",
             "CatalogService", "BookingService", "NotificationService", "MaintenanceJobs", "PricingCalculator",
             "RefundCalculator", "ShowSeatRepository", "TheaterSeat", "MovieShow", "BookingSeat", "ShowSeat",
             "ApiModels", "ApiException", "ApiErrors", "createShow", "browseShows", "seatPrice", "applyDiscount",
             "lockOne", "lockByBooking", "expireOne", "enqueueReminders", "deliverPending", "BigDecimal"]
    for name in sorted(names, key=len, reverse=True):
        spoken = re.sub(r"(?<=[a-z])(?=[A-Z])", " ", name)
        text = re.sub(r"\b" + re.escape(name) + r"\b", spoken, text)
    return text.replace("JDBC", "J D B C").replace("HTTP", "H T T P").replace("API", "A P I").replace("Api ", "A P I ").replace("SMS", "S M S")


def prepare():
    WORK.mkdir(parents=True, exist_ok=True)
    for item in segments():
        paths(item)["text"].write_text(speech_text(item["text"]))
    print(f"Prepared {len(segments())} narration segments", flush=True)


def duration(path):
    result = subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "stream=duration",
                                      "-of", "json", str(path)], text=True)
    values = [float(s.get("duration", 0)) for s in json.loads(result)["streams"]]
    return max(values, default=0)


def narrate():
    prepare()
    for item in segments():
        p = paths(item)
        fingerprint = hashlib.sha256((p["text"].read_text() + DATA["voice"] + str(DATA["words_per_minute"])).encode()).hexdigest()
        cached = p["hash"].exists() and p["hash"].read_text() == fingerprint and p["aiff"].exists()
        if not cached or duration(p["aiff"]) <= 0:
            run(["say", "-v", DATA["voice"], "-r", DATA["words_per_minute"], "-f", p["text"], "-o", p["aiff"]])
            if duration(p["aiff"]) <= 0:
                raise RuntimeError("Speech synthesis produced empty audio. Allow access to the macOS speech service and retry.")
            p["hash"].write_text(fingerprint)
        print(f"Narrated {item['number']:02d}/{len(segments())}: {duration(p['aiff']):.1f}s", flush=True)


def wrap_pixels(draw, text, face, width):
    lines, current = [], ""
    for word in text.split():
        candidate = f"{current} {word}".strip()
        if current and draw.textlength(candidate, font=face) > width:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def label(draw, xy, text, size=24, color=TEXT, mono=False):
    draw.text(xy, text, font=font(size, mono), fill=color)


def box(draw, coords, fill=PANEL, outline="#26364C", radius=14):
    draw.rounded_rectangle(coords, radius=radius, fill=fill, outline=outline, width=2)


def arrow(draw, x, y):
    draw.line((x, y, x+29, y), fill=TEAL, width=3)
    draw.polygon(((x+33,y),(x+23,y-8),(x+23,y+8)), fill=TEAL)


def code_rows(item, chars):
    source = (ROOT / item["source"]).read_text().splitlines()
    chosen = [(n, source[n-1]) for a, b in item["ranges"] for n in range(a, b+1)]
    nonempty = [len(line) - len(line.lstrip()) for _, line in chosen if line.strip()]
    indent = min(nonempty, default=0)
    result, previous = [], None
    for number, line in chosen:
        if previous is not None and number != previous + 1:
            result.append((None, "…", False))
        clean = line[indent:] if len(line) >= indent else ""
        continuation = " " * min(len(clean) - len(clean.lstrip()) + 4, 12)
        pieces = textwrap.wrap(clean, width=chars, subsequent_indent=continuation,
                               replace_whitespace=False, break_long_words=True, break_on_hyphens=False) or [""]
        for i, piece in enumerate(pieces):
            result.append((number if i == 0 else "↳", piece, number in item.get("highlights", [])))
        previous = number
    return result


TOKEN = re.compile(r'//.*|"(?:\\.|[^"\\])*"|@[A-Za-z]+|\b(?:public|private|protected|static|final|class|interface|record|return|new|if|else|for|try|catch|throw|void|boolean|null|true|false)\b|\b\d+(?:\.\d+)?\b|\b[A-Z][A-Za-z0-9_]*\b')


def syntax(draw, x, y, line, face):
    pos = 0
    for match in TOKEN.finditer(line):
        prefix = line[pos:match.start()]
        draw.text((x, y), prefix, font=face, fill="#DCE5F3")
        x += draw.textlength(prefix, font=face)
        token = match.group()
        color = "#7F94AF" if token.startswith("//") else "#B7E29A" if token.startswith('"') else TEAL if token.startswith("@") else "#DC9CF7" if token[0].islower() else "#F3C98B" if token[0].isdigit() else BLUE
        draw.text((x, y), token, font=face, fill=color)
        x += draw.textlength(token, font=face)
        pos = match.end()
    draw.text((x, y), line[pos:], font=face, fill="#DCE5F3")


def code_panel(draw, item):
    box(draw, (64, 309, 1856, 864), fill="#101A29")
    short = item["source"].replace("src/main/java/com/example/movietickets/", "").replace("src/test/java/com/example/movietickets/", "test/")
    label(draw, (90, 325), short, 23, BLUE, True)
    label(draw, (1510, 328), "ACTUAL SOURCE", 19, MUTED)
    draw.line((88, 368, 1832, 368), fill="#29374B", width=2)
    selected = None
    for size in (27, 26, 25, 24, 23, 22, 21):
        face = font(size, True)
        chars = int(1650 / draw.textlength("M", font=face))
        rows = code_rows(item, chars)
        line_h = size + 5
        if len(rows) * line_h <= 472:
            selected = (face, rows, line_h, size)
            break
    if selected is None:
        raise ValueError(f"Too many code lines in segment {item['number']}")
    face, rows, line_h, size = selected
    y = 381
    for number, line, highlighted in rows:
        if highlighted:
            draw.rectangle((82, y-1, 1838, y+line_h-1), fill="#1D3546")
            draw.rectangle((82, y-1, 86, y+line_h-1), fill=TEAL)
        label(draw, (96, y), str(number) if number is not None else "", size-3, "#6F859D", True)
        syntax(draw, 164, y, line, face)
        if draw.textlength(line, font=face) > 1650:
            raise ValueError(f"Code overflow in segment {item['number']}")
        y += line_h
    return size


def architecture(draw):
    box(draw, (64, 309, 1856, 864))
    label(draw, (96, 332), "REQUEST FLOW", 22, MUTED)
    titles = ["Controller", "Service", "Repository", "PostgreSQL"]
    details = ["HTTP + validation", "Rules + transactions", "Persistence + locks", "Durable state"]
    for i, (title, detail) in enumerate(zip(titles, details)):
        x = 112 + i * 440
        box(draw, (x, 421, x+378, 568), fill="#182C42", outline="#33516B")
        label(draw, (x+24, 447), title, 37, TEAL if i == 1 else TEXT)
        label(draw, (x+24, 511), detail, 25, MUTED)
        if i < 3:
            arrow(draw, x+396, 490)
    label(draw, (112, 649), "SUPPORTING PACKAGES", 21, MUTED)
    label(draw, (112, 701), "dto  /  entity  /  config  /  jobs  /  error  /  util", 30, BLUE, True)
    label(draw, (112, 772), "One Spring Boot process. Database-backed booking coordination.", 29, TEXT)


def model(draw):
    box(draw, (64, 309, 1856, 864))
    label(draw, (96, 332), "CORE RELATIONSHIPS", 22, MUTED)
    rows = [
        ("City", "Theater", "MovieShow", "ShowSeat"),
        ("Movie", "MovieShow", "Booking", "BookingSeat"),
    ]
    for row, names in enumerate(rows):
        y = 405 + row * 160
        for i, name in enumerate(names):
            x = 110 + i*440
            box(draw, (x, y, x+378, y+103), fill="#182C42", outline="#33516B")
            label(draw, (x+22, y+28), name, 31, TEAL if name in ("ShowSeat", "Booking") else TEXT, True)
            if i < 3:
                arrow(draw, x+396, y+51)
    label(draw, (112, 748), "Payment / Refund / Notification", 31, BLUE, True)
    label(draw, (810, 752), "record outcomes for a booking", 28, MUTED)


def verification(draw):
    box(draw, (64, 309, 1856, 864))
    label(draw, (96, 334), "VERIFICATION", 22, MUTED)
    label(draw, (112, 399), "17", 118, TEAL)
    label(draw, (307, 448), "tests passed", 42, TEXT)
    label(draw, (810, 408), "1 optional PostgreSQL test skipped", 36, BLUE)
    label(draw, (810, 473), "No live database credentials supplied", 27, MUTED)
    label(draw, (112, 614), "mvn test", 40, TEXT, True)
    label(draw, (112, 699), "PostgreSQL row-lock verification remains pending.", 31, MUTED)
    label(draw, (112, 765), "Source tests: integration/ + util/", 27, BLUE, True)


def render(item, start=0, end=0, total=1):
    scene = DATA["scenes"][item["chapter"]]
    image = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 14, H), fill=TEAL)
    label(draw, (64, 32), "MOVIE TICKET BOOKING  /  CODE WALKTHROUGH", 21, TEAL)
    label(draw, (1723, 31), f"{item['chapter']+1:02d} / 12", 23, MUTED, True)
    title_face = font(52)
    if draw.textlength(scene["title"], font=title_face) > 1792:
        raise ValueError("Title overflow")
    draw.text((64, 81), scene["title"], font=title_face, fill=TEXT)
    label(draw, (67, 153), scene["subtitle"], 25, MUTED)
    for i, point in enumerate(scene["points"]):
        x = 64 + i * 604
        box(draw, (x, 210, x+584, 288), fill="#14263A", outline="#244059", radius=10)
        label(draw, (x+18, 231), f"0{i+1}", 23, TEAL, True)
        face = font(25)
        while draw.textlength(point, font=face) > 490:
            face = font(face.size-1)
        draw.text((x+73, 231), point, font=face, fill=TEXT)
    kind = item.get("kind", "code")
    code_size = None
    if kind == "architecture":
        architecture(draw)
    elif kind == "model":
        model(draw)
    elif kind == "verification":
        verification(draw)
    else:
        code_size = code_panel(draw, item)
    box(draw, (64, 884, 1856, 1008), fill="#18263A", outline="#18263A", radius=12)
    face = font(33)
    lines = wrap_pixels(draw, item["text"], face, 1704)
    if len(lines) > 2:
        face = font(30)
        lines = wrap_pixels(draw, item["text"], face, 1704)
    if len(lines) > 2:
        raise ValueError(f"Caption overflow in segment {item['number']}: {lines}")
    y = 901 + (40 if len(lines) == 1 else 0)/2
    for line in lines:
        draw.text((106, y), line, font=face, fill=TEXT)
        y += 44
    label(draw, (64, 1025), "Synthetic English narration · source code excerpts", 18, MUTED)
    stamp = f"{int(start)//60:02d}:{int(start)%60:02d}"
    label(draw, (1739, 1024), stamp, 20, MUTED, True)
    for i in range(12):
        x = 64 + i*151
        draw.rounded_rectangle((x, 1061, x+139, 1069), radius=3,
                               fill=TEAL if i <= item["chapter"] else "#29364B")
    image.save(paths(item)["frame"])
    return code_size


def preview():
    prepare()
    for item in segments():
        size = render(item)
        print(f"Frame {item['number']:02d}: code font {size or 'diagram'}", flush=True)
    images = [Image.open(paths(scene_items[0])["frame"]).resize((480,270))
              for scene in range(12) if (scene_items := [x for x in segments() if x["chapter"] == scene])]
    contact = Image.new("RGB", (1440, 1080), BG)
    for i, thumb in enumerate(images):
        contact.paste(thumb, ((i%3)*480, (i//3)*270))
    contact.save(WORK / "contact-sheet.png")


def srt_time(seconds):
    millis = round(seconds * 1000)
    return f"{millis//3600000:02d}:{millis//60000%60:02d}:{millis//1000%60:02d},{millis%1000:03d}"


def build():
    prepare()
    items = segments()
    time = 0.0
    captions, concat, manifest = [], ["ffconcat version 1.0"], []
    chapter_starts = []
    output_audio = WORK / "narration.wav"
    with wave.open(str(output_audio), "wb") as combined:
        combined.setnchannels(1)
        combined.setsampwidth(2)
        combined.setframerate(RATE)
        for item in items:
            p = paths(item)
            if not p["aiff"].exists() or duration(p["aiff"]) <= 0:
                raise RuntimeError(f"Missing narration for segment {item['number']}; run narrate first")
            run(["ffmpeg", "-v", "error", "-y", "-i", p["aiff"], "-ar", RATE, "-ac", 1, "-c:a", "pcm_s16le", p["wav"]])
            with wave.open(str(p["wav"]), "rb") as audio:
                samples = audio.getnframes()
                pcm = audio.readframes(samples)
            lead = 0.28 if item["step"] == 0 else 0.12
            tail = 0.30 if item["number"] < len(items) else 1.0
            seconds = math.ceil((samples/RATE + lead + tail)*FPS)/FPS
            combined.writeframes(b"\x00\x00" * round(lead*RATE))
            combined.writeframes(pcm)
            remaining = round(seconds*RATE) - round(lead*RATE) - samples
            combined.writeframes(b"\x00\x00" * remaining)
            if item["step"] == 0:
                chapter_starts.append((time, DATA["scenes"][item["chapter"]]["title"]))
            code_size = render(item, time, time+seconds)
            concat += [f"file '{p['frame']}'", f"duration {seconds:.9f}"]
            captions.append(f"{item['number']}\n{srt_time(time+lead)} --> {srt_time(time+lead+samples/RATE)}\n{item['text']}\n")
            source_info = {}
            if "source" in item:
                source_info = {"source": item["source"], "ranges": item["ranges"],
                               "source_sha256": hashlib.sha256((ROOT/item["source"]).read_bytes()).hexdigest()}
            manifest.append({**source_info, "segment": item["number"], "chapter": item["chapter"]+1,
                             "start": round(time,3), "end": round(time+seconds,3), "text": item["text"],
                             "code_font_px": code_size})
            time += seconds
    concat.append(f"file '{paths(items[-1])['frame']}'")
    (WORK / "frames.ffconcat").write_text("\n".join(concat)+"\n")
    (HERE / "walkthrough.srt").write_text("\n".join(captions))
    metadata = [";FFMETADATA1", "title=Movie Ticket Booking - Code Walkthrough", "comment=Synthetic narration with actual source excerpts"]
    transcript = ["# Movie Ticket Booking code walkthrough", "", "Synthetic English narration; source excerpts are taken from this project.", ""]
    for i, (start, title) in enumerate(chapter_starts):
        end = chapter_starts[i+1][0] if i+1 < len(chapter_starts) else time
        metadata += ["[CHAPTER]", "TIMEBASE=1/1000", f"START={round(start*1000)}", f"END={round(end*1000)}", f"title={title}"]
        transcript += [f"## {int(start)//60:02d}:{int(start)%60:02d} — {title}", ""]
        transcript += [entry["text"] for entry in manifest if entry["chapter"] == i+1]
        transcript.append("")
    (WORK / "chapters.ffmetadata").write_text("\n".join(metadata)+"\n")
    (HERE / "transcript.md").write_text("\n\n".join(transcript))
    (HERE / "render-manifest.json").write_text(json.dumps({"duration_seconds": round(time,3), "resolution": [W,H],
        "fps": FPS, "voice": DATA["voice"], "words_per_minute": DATA["words_per_minute"], "segments": manifest}, indent=2)+"\n")
    output = HERE / "movie-ticket-booking-walkthrough.mp4"
    run(["ffmpeg", "-v", "warning", "-y", "-f", "concat", "-safe", "0", "-i", WORK/"frames.ffconcat",
         "-i", output_audio, "-i", WORK/"chapters.ffmetadata", "-map", "0:v:0", "-map", "1:a:0",
         "-map_metadata", "2", "-map_chapters", "2", "-vf", f"fps={FPS},format=yuv420p",
         "-c:v", "libx264", "-preset", "veryfast", "-crf", "18", "-threads", "4",
         "-af", "loudnorm=I=-16:TP=-1.5:LRA=7", "-ar", RATE, "-c:a", "aac", "-b:a", "128k",
         "-t", f"{time:.6f}", "-movflags", "+faststart", output])
    print(f"Built {output.name}: {time:.1f}s, {output.stat().st_size/1024/1024:.1f} MiB", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("prepare", "narrate", "preview", "build"))
    mode = parser.parse_args().mode
    {"prepare": prepare, "narrate": narrate, "preview": preview, "build": build}[mode]()
