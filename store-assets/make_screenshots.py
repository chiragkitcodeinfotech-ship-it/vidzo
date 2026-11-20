"""
Builds the Play Store screenshots (2160 x 3840, 9:16 "4K") from real app captures.

    python3 store-assets/make_screenshots.py

Input:  store-assets/raw/*.png   (captured on the Pixel 8 emulator at 1440 x 3200)
Output: store-assets/screenshots/*.png
Needs:  Google Chrome (renders the HTML layout), internet for the Google Font.
"""
import html
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "raw"
OUT = ROOT / "screenshots"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
WIDTH, HEIGHT = 2160, 3840

# file, headline (text inside * * is coloured), sub line, layout, footnote
SLIDES = [
    ("1_home.png", "Watch. Browse. *Enjoy.*", "Your favourite sites and videos in one fast, light browser", "portrait", ""),
    ("2_video.png", "Videos in *full screen*", "Turn your phone and enjoy smooth, distraction-free playback", "landscape",
     "Video: Big Buck Bunny © Blender Foundation, CC BY 3.0"),
    ("3_tabs.png", "All your tabs, *one tap away*", "Keep many pages open and switch in a second", "portrait", ""),
    ("4_search.png", "Find anything *faster*", "Smart suggestions appear as you type", "portrait", ""),
    ("5_settings.png", "Make it *yours*", "Dark mode, search engine, default browser and more", "portrait", ""),
    ("6_pin.png", "Private with a *PIN lock*", "Only you can open your browser", "portrait", ""),
]

CSS = """
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@500;600;800&display=block');
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: 2160px; height: 3840px; overflow: hidden; }
body {
  background: #F4F3FA;
  font-family: 'Plus Jakarta Sans', -apple-system, 'Helvetica Neue', sans-serif;
  color: #12131A;
  position: relative;
}
.brand { position: absolute; top: 190px; left: 0; right: 0; text-align: center;
  font-size: 54px; font-weight: 600; letter-spacing: 6px; color: #6B6E80; text-transform: uppercase; }
h1 { position: absolute; top: 300px; left: 140px; right: 140px; text-align: center;
  font-size: 168px; line-height: 1.08; font-weight: 800; letter-spacing: -4px; }
h1 em { font-style: normal; color: #6A4DF5; }
p.sub { position: absolute; top: 700px; left: 220px; right: 220px; text-align: center;
  font-size: 66px; line-height: 1.35; font-weight: 500; color: #555869; }
.phone { position: absolute; background: #0B0B0E; box-shadow: 0 60px 140px rgba(30, 20, 80, .22); }
.phone img { display: block; width: 100%; height: 100%; object-fit: cover; }
.portrait { left: 290px; top: 1060px; width: 1580px; height: 3511px; border-radius: 150px; padding: 34px; }
.portrait img { border-radius: 118px; }
.landscape { left: 110px; top: 1500px; width: 1940px; height: 893px; border-radius: 110px; padding: 30px; }
.landscape img { border-radius: 82px; }
.note { position: absolute; bottom: 120px; left: 0; right: 0; text-align: center;
  font-size: 40px; font-weight: 500; color: #8B8EA0; }
.chips { position: absolute; top: 2620px; left: 0; right: 0; display: flex; justify-content: center; gap: 36px; }
.chip { background: #fff; border-radius: 80px; padding: 34px 58px; font-size: 56px; font-weight: 600;
  color: #2A2C38; box-shadow: 0 12px 40px rgba(30, 20, 80, .08); }
"""


def headline(text: str) -> str:
    parts = html.escape(text).split("*")
    return "".join(f"<em>{p}</em>" if i % 2 else p for i, p in enumerate(parts))


def slide_html(image: Path, title: str, sub: str, layout: str, note: str) -> str:
    chips = ""
    if layout == "landscape":
        chips = '<div class="chips"><div class="chip">Full screen</div><div class="chip">Auto rotate</div>' \
                '<div class="chip">Smooth playback</div></div>'
    note_html = f'<div class="note">{html.escape(note)}</div>' if note else ""
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>{CSS}</style></head><body>
<div class="brand">Vidzo Browser</div>
<h1>{headline(title)}</h1>
<p class="sub">{html.escape(sub)}</p>
<div class="phone {layout}"><img src="{image.as_uri()}"></div>
{chips}{note_html}
</body></html>"""


def render(page: str, out: Path) -> None:
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False) as f:
        f.write(page)
        src = Path(f.name)
    try:
        subprocess.run(
            [CHROME, "--headless=new", "--hide-scrollbars", "--force-device-scale-factor=1",
             f"--window-size={WIDTH},{HEIGHT}", "--virtual-time-budget=8000",
             "--allow-file-access-from-files", f"--screenshot={out}", src.as_uri()],
            check=True, capture_output=True,
        )
    finally:
        src.unlink(missing_ok=True)


def main() -> None:
    OUT.mkdir(exist_ok=True)
    for name, title, sub, layout, note in SLIDES:
        image = RAW / name
        if not image.exists():
            print(f"skip {name} (no capture yet)")
            continue
        out = OUT / name
        render(slide_html(image, title, sub, layout, note), out)
        print(f"wrote {out.relative_to(ROOT.parent)}")


if __name__ == "__main__":
    main()
