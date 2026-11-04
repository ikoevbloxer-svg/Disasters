#!/usr/bin/env python3
"""
Генератор ресурсов мода (текстуры и звуки).
Запуск из корня проекта:  python3 tools/gen_assets.py path/to/scary.jpg
Нужны: Pillow, numpy, ffmpeg (с libvorbis).
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "vladdisasters")
TEX = os.path.join(ROOT, "textures")
SND = os.path.join(ROOT, "sounds")
rng = np.random.default_rng(1337)


# ============================================================ helpers
def jitter(c, amt=12):
    return tuple(int(max(0, min(255, v + rng.integers(-amt, amt + 1)))) for v in c) + (255,)


def fill(img, x, y, w, h, color, amt=10):
    px = img.load()
    for i in range(x, x + w):
        for j in range(y, y + h):
            px[i, j] = jitter(color, amt)


def put(img, x, y, color):
    img.load()[x, y] = tuple(color) + (255,) if len(color) == 3 else color


def box_regions(u, v, w, h, d):
    """Стандартная развёртка куба Minecraft."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def paint_box(img, u, v, w, h, d, color, amt=10):
    for (x, y, ww, hh) in box_regions(u, v, w, h, d).values():
        fill(img, x, y, ww, hh, color, amt)


# ============================================================ Mama Vlad texture
SKIN = (236, 184, 152)
SKIN_D = (214, 156, 128)
DRESS = (112, 32, 74)
DRESS_DOT = (238, 206, 92)
DRESS_DOT2 = (246, 246, 236)
HAIR = (214, 82, 30)
HAIR_D = (172, 56, 18)
HAIR_L = (240, 120, 52)
SLIPPER = (244, 150, 186)
LIP = (204, 28, 54)
MOUTH = (96, 12, 24)


def hair_box(img, u, v, w, h, d):
    for (x, y, ww, hh) in box_regions(u, v, w, h, d).values():
        for i in range(x, x + ww):
            stripe = (i * 7) % 5
            base = HAIR if stripe < 3 else (HAIR_D if stripe == 3 else HAIR_L)
            for j in range(y, y + hh):
                put(img, i, j, jitter(base, 10))


def dress_region(img, x, y, w, h):
    for i in range(x, x + w):
        for j in range(y, y + h):
            c = DRESS
            if (i + 2 * j) % 7 == 0:
                c = DRESS_DOT
            elif (2 * i + j) % 11 == 0:
                c = DRESS_DOT2
            put(img, i, j, jitter(c, 8))


def mama_texture(angry):
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    # Тело (0,0) 22x16x16 и живот (0,32) 18x11x3 — платье в горошек
    for r in box_regions(0, 0, 22, 16, 16).values():
        dress_region(img, *r)
    for r in box_regions(0, 32, 18, 11, 3).values():
        dress_region(img, *r)
    # Фартук на животе
    fx, fy, fw, fh = box_regions(0, 32, 18, 11, 3)["front"]
    fill(img, fx + 3, fy + 1, fw - 6, fh - 2, (240, 236, 226), 6)
    # Сердечко на фартуке
    hx, hy = fx + fw // 2 - 2, fy + 3
    for (a, b) in [(0, 0), (1, 0), (3, 0), (4, 0), (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (1, 2), (2, 2), (3, 2), (2, 3)]:
        put(img, hx + a, hy + b, (210, 30, 60))

    # Голова (76,0) 12x12x12: кожа, затылок и макушка под волосами
    paint_box(img, 76, 0, 12, 12, 12, SKIN, 8)
    reg = box_regions(76, 0, 12, 12, 12)
    for k in ("top", "back"):
        x, y, w, h = reg[k]
        fill(img, x, y, w, h, HAIR, 12)
    for k in ("right", "left"):
        x, y, w, h = reg[k]
        fill(img, x, y, w, 5, HAIR, 12)
        put(img, x + w // 2, y + 8, (250, 210, 60))  # серёжка
        put(img, x + w // 2, y + 9, (250, 210, 60))

    # Лицо
    x0, y0, _, _ = reg["front"]
    face = img.load()
    # чёлка
    for i in range(12):
        depth = 2 + (1 if i % 3 == 0 else 0)
        for j in range(depth):
            face[x0 + i, y0 + j] = jitter(HAIR, 10)
    brow = (80, 34, 16)
    if angry:
        for (i, j) in [(2, 3), (3, 3), (4, 4), (9, 3), (8, 3), (7, 4)]:
            face[x0 + i, y0 + j] = brow + (255,)
    else:
        for i in (2, 3, 4, 7, 8, 9):
            face[x0 + i, y0 + 3] = brow + (255,)
    # тени для век
    for i in (2, 3, 4, 7, 8, 9):
        face[x0 + i, y0 + 4] = jitter((90, 150, 220), 8) if not angry else jitter((60, 20, 40), 6)
    # глаза
    for (ex) in (2, 7):
        for i in range(3):
            face[x0 + ex + i, y0 + 5] = (250, 250, 250, 255)
        pupil = (255, 20, 20, 255) if angry else (40, 90, 40, 255)
        face[x0 + ex + 1, y0 + 5] = pupil
        if angry:
            face[x0 + ex, y0 + 5] = (255, 120, 60, 255)
    # румяна
    for (i, j) in [(1, 7), (2, 7), (9, 7), (10, 7), (1, 8), (10, 8)]:
        face[x0 + i, y0 + j] = jitter((226, 112, 112), 6)
    # губы и рот (за челюстью)
    for i in range(4, 8):
        face[x0 + i, y0 + 8] = LIP + (255,)
    for i in range(3, 9):
        for j in (9, 10):
            face[x0 + i, y0 + j] = MOUTH + (255,)
    if angry:  # крошки еды вокруг рта
        for (i, j) in [(3, 8), (8, 8), (2, 9), (9, 10)]:
            face[x0 + i, y0 + j] = (150, 110, 70, 255)
    # двойной подбородок
    for i in range(2, 10):
        face[x0 + i, y0 + 11] = jitter(SKIN_D, 6)

    # Нос (100,50) 2x3x2
    paint_box(img, 100, 50, 2, 3, 2, (226, 160, 132), 6)
    if angry:
        nx, ny, _, _ = box_regions(100, 50, 2, 3, 2)["front"]
        put(img, nx, ny + 2, (120, 60, 60))

    # Челюсть (100,44) 8x3x2: сверху — рот, спереди — губа и подбородок
    paint_box(img, 100, 44, 8, 3, 2, SKIN, 6)
    jr = box_regions(100, 44, 8, 3, 2)
    x, y, w, h = jr["top"]
    fill(img, x, y, w, h, MOUTH, 6)
    x, y, w, h = jr["front"]
    for i in range(w):
        put(img, x + i, y, LIP)
    for i in range(1, w - 1):  # зубы
        if i % 2 == 1:
            put(img, x + i, y, (240, 236, 220) if not angry else (230, 220, 160))
    x, y, w, h = jr["back"]
    fill(img, x, y, w, h, MOUTH, 6)

    # Волосы
    hair_box(img, 44, 32, 14, 4, 14)
    hair_box(img, 0, 50, 14, 16, 4)
    hair_box(img, 36, 52, 2, 12, 10)
    hair_box(img, 60, 52, 2, 12, 10)
    hair_box(img, 100, 32, 6, 4, 6)
    # заколка на пучке
    bx, by, bw, bh = box_regions(100, 32, 6, 4, 6)["front"]
    for i in range(bw):
        put(img, bx + i, by + 2, (60, 200, 160))

    # Руки (0,74) и (24,74): 6x16x6 — короткий рукав, кожа, кисть
    for u in (0, 24):
        for name, (x, y, w, h) in box_regions(u, 74, 6, 16, 6).items():
            for j in range(h):
                for i in range(w):
                    if name == "top":
                        c = DRESS
                    elif name == "bottom":
                        c = SKIN_D
                    elif j < 5:
                        c = DRESS if (i + j) % 5 else DRESS_DOT
                    elif j >= h - 2:
                        c = SKIN_D
                    else:
                        c = SKIN
                    put(img, x + i, y + j, jitter(c, 8))
    # Ноги (48,74) и (72,74): 6x10x6 — подол, кожа, розовые тапки
    for u in (48, 72):
        regs = box_regions(u, 74, 6, 10, 6)
        for name, (x, y, w, h) in regs.items():
            for j in range(h):
                for i in range(w):
                    if name == "top":
                        c = DRESS
                    elif name == "bottom":
                        c = SLIPPER
                    elif j < 3:
                        c = DRESS
                    elif j >= h - 3:
                        c = SLIPPER if (i + j) % 3 else (255, 200, 220)
                    else:
                        c = SKIN
                    put(img, x + i, y + j, jitter(c, 8))
    if angry:
        # пятна от еды на платье
        for _ in range(26):
            x = int(rng.integers(16, 60)); y = int(rng.integers(16, 32))
            put(img, x, y, (90, 60, 30))
    return img


# ============================================================ items
def kolbasa_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # батон колбасы по диагонали
    for t in range(2, 13):
        cx, cy = t, 15 - t
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if dx * dx + dy * dy <= 5:
                    x, y = cx + dx, cy + dy - 1
                    if 0 <= x < 16 and 0 <= y < 16:
                        shade = 1.0 - 0.12 * (dx - dy) / 2
                        base = np.array((176, 52, 60)) * shade
                        px[x, y] = jitter(tuple(int(v) for v in base), 8)
    # срез
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx * dx + dy * dy <= 5:
                x, y = 13 + dx, 2 + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    px[x, y] = jitter((232, 128, 138), 8)
    for (x, y) in [(12, 2), (14, 1), (13, 3), (14, 3)]:
        px[x, y] = (250, 238, 230, 255)  # кусочки сала
    # верёвочка
    px[1, 13] = (220, 200, 150, 255)
    px[0, 14] = (220, 200, 150, 255)
    px[0, 15] = (200, 180, 130, 255)
    # контур
    out = img.copy()
    po = out.load()
    for x in range(16):
        for y in range(16):
            if px[x, y][3] == 0:
                for (dx, dy) in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] != 0:
                        po[x, y] = (70, 18, 24, 255)
                        break
    return out


def identifier_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # корпус
    for x in range(2, 14):
        for y in range(4, 15):
            px[x, y] = jitter((96, 100, 108), 6)
    for x in range(2, 14):
        px[x, 4] = (140, 146, 156, 255)
        px[x, 14] = (60, 62, 68, 255)
    for y in range(4, 15):
        px[2, y] = (130, 136, 146, 255)
        px[13, y] = (60, 62, 68, 255)
    # экран-радар
    cx, cy = 7.5, 9.0
    for x in range(4, 12):
        for y in range(6, 12):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            c = (16, 60, 24)
            if abs(d - 2.5) < 0.5 or abs(d - 1.0) < 0.4:
                c = (40, 150, 60)
            if abs((x - cx) - (y - cy) * -0.9) < 0.6 and x >= cx and y <= cy:
                c = (120, 255, 140)
            px[x, y] = c + (255,)
    px[9, 7] = (255, 40, 40, 255)  # бедствие на радаре!
    # антенна
    for y in range(0, 4):
        px[11, y] = (170, 170, 176, 255)
    px[11, 0] = (255, 60, 60, 255)
    # кнопки
    px[4, 13] = (220, 40, 40, 255)
    px[6, 13] = (240, 200, 40, 255)
    px[8, 13] = (60, 200, 80, 255)
    return img


# ============================================================ sounds
SR = 22050


def tone_env(n, attack=0.02, release=0.2):
    t = np.arange(n) / SR
    dur = n / SR
    env = np.minimum(1, t / max(attack, 1e-4)) * np.minimum(1, (dur - t) / max(release, 1e-4))
    return np.clip(env, 0, 1)


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def osc_saw(freq):
    phase = np.cumsum(freq) / SR
    return 2 * (phase - np.floor(phase + 0.5))


def osc_sin(freq):
    return np.sin(2 * np.pi * np.cumsum(freq) / SR)


def norm(x, peak=0.95):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def scream(dur=1.7, f0=520, f1=920, f2=640):
    n = int(SR * dur)
    t = np.arange(n) / SR
    rise = np.interp(t, [0, dur * 0.25, dur * 0.7, dur], [f0, f1, f2 * 1.1, f2])
    vib = 1 + 0.06 * np.sin(2 * np.pi * 7.5 * t) + 0.02 * np.sin(2 * np.pi * 13 * t)
    f = rise * vib
    x = 0.6 * osc_saw(f) + 0.3 * osc_sin(f * 2) + 0.15 * osc_sin(f * 3.01)
    x += 0.25 * rng.standard_normal(n) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * t))
    x = np.tanh(x * 2.6)
    return norm(x * tone_env(n, 0.04, 0.35))


def roar(dur=1.9):
    n = int(SR * dur)
    t = np.arange(n) / SR
    f = np.interp(t, [0, 0.3, dur], [95, 130, 70]) * (1 + 0.04 * np.sin(2 * np.pi * 5 * t))
    growl = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 26 * t))
    x = osc_saw(f) * 0.8 + osc_saw(f * 1.5) * 0.3
    noise = lowpass(rng.standard_normal(n), 900) * 3
    x = (x + noise) * (0.55 + 0.45 * growl)
    x = np.tanh(x * 3)
    return norm(x * tone_env(n, 0.06, 0.5))


def chomp():
    parts = []
    for k in range(4):
        n = int(SR * 0.18)
        t = np.arange(n) / SR
        crunch = rng.standard_normal(n) * np.exp(-t * 28)
        thump = np.sin(2 * np.pi * 85 * t) * np.exp(-t * 18)
        parts.append(crunch * 0.7 + thump)
        parts.append(np.zeros(int(SR * (0.05 + 0.04 * k))))
    x = np.concatenate(parts)
    return norm(np.tanh(x * 2))


def hurt():
    n = int(SR * 0.45)
    t = np.arange(n) / SR
    f = np.interp(t, [0, 0.45], [760, 360])
    x = np.tanh((osc_saw(f) + 0.3 * rng.standard_normal(n)) * 2.5)
    return norm(x * tone_env(n, 0.01, 0.2))


def death():
    a = scream(1.6, 820, 700, 180)
    b = roar(1.4)
    return norm(np.concatenate([a, b * 0.9]))


def screamer_snd():
    dur = 1.7
    n = int(SR * dur)
    t = np.arange(n) / SR
    f = 2200 + 900 * np.sin(2 * np.pi * 11 * t) + 400 * rng.standard_normal(n).cumsum() / (n ** 0.5)
    x = osc_saw(np.abs(f)) + 0.8 * osc_sin(f * 0.51) + 1.4 * rng.standard_normal(n)
    x += 0.8 * osc_saw(np.interp(t, [0, dur], [700, 1300]))
    x = np.clip(x * 4, -1, 1)
    env = np.minimum(1, t / 0.005) * np.minimum(1, (dur - t) / 0.25)
    return norm(x * env, 1.0)


def siren():
    dur = 4.5
    n = int(SR * dur)
    t = np.arange(n) / SR
    f = np.interp(t, [0, 1.6, 3.2, dur], [260, 820, 820, 380])
    x = osc_sin(f) + 0.35 * osc_sin(f * 2) + 0.15 * osc_saw(f)
    return norm(np.tanh(x * 1.4) * tone_env(n, 0.2, 0.8))


def alarm():
    out = []
    for _ in range(2):
        n = int(SR * 0.14)
        t = np.arange(n) / SR
        out.append(np.sign(np.sin(2 * np.pi * 980 * t)) * 0.8)
        out.append(np.zeros(int(SR * 0.08)))
    return norm(np.concatenate(out))


def boom(dur=6.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    crack = rng.standard_normal(n) * np.exp(-t * 9)
    rumble = lowpass(rng.standard_normal(n), 110) * 12 * np.exp(-t * 0.55)
    sub = np.sin(2 * np.pi * np.interp(t, [0, dur], [45, 25]) * t) * np.exp(-t * 0.9)
    x = crack * 0.7 + rumble + sub
    return norm(np.tanh(x * 1.6))


def rumble(dur=3.2):
    n = int(SR * dur)
    t = np.arange(n) / SR
    x = lowpass(rng.standard_normal(n), 70) * 10
    am = 0.6 + 0.4 * np.sin(2 * np.pi * 1.7 * t) * np.sin(2 * np.pi * 0.6 * t)
    return norm(np.tanh(x * am * 1.5) * tone_env(n, 0.4, 0.8))


def wind(dur=3.5):
    n = int(SR * dur)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    a = lowpass(noise, 700) - lowpass(noise, 180)
    howl = osc_sin(np.interp(t, [0, dur], [240, 330]) * (1 + 0.1 * np.sin(2 * np.pi * 0.8 * t))) * 0.15
    am = 0.5 + 0.5 * np.sin(2 * np.pi * 0.5 * t + 1)
    return norm((a * 4 + howl) * am * tone_env(n, 0.5, 0.8))


def hum(dur=3.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    x = osc_sin(np.full(n, 42.0)) + osc_sin(np.full(n, 44.5)) + 0.5 * osc_saw(np.full(n, 63.0))
    whine = 0.12 * osc_sin(np.interp(t, [0, dur], [1600, 1900]))
    return norm(np.tanh((x + whine) * 1.2) * tone_env(n, 0.5, 0.6))


def whoosh(dur=2.2):
    n = int(SR * dur)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    hi = noise - lowpass(noise, 600)
    lo = lowpass(noise, 260)
    mix = np.interp(t, [0, dur], [1, 0])
    x = hi * mix + lo * 3 * (1 - mix)
    env = np.interp(t, [0, dur * 0.7, dur], [0.1, 1, 0])
    return norm(x * env)


def write_ogg(name, data):
    os.makedirs(SND, exist_ok=True)
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        tmp = f.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    out = os.path.join(SND, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    os.remove(tmp)


def main():
    os.makedirs(os.path.join(TEX, "entity"), exist_ok=True)
    os.makedirs(os.path.join(TEX, "item"), exist_ok=True)
    os.makedirs(os.path.join(TEX, "gui"), exist_ok=True)
    mama_texture(False).save(os.path.join(TEX, "entity", "mama_vlad.png"))
    mama_texture(True).save(os.path.join(TEX, "entity", "mama_vlad_angry.png"))
    Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(os.path.join(TEX, "entity", "white.png"))
    kolbasa_icon().save(os.path.join(TEX, "item", "kolbasa.png"))
    identifier_icon().save(os.path.join(TEX, "item", "disaster_identifier.png"))

    if len(sys.argv) > 1:
        src = Image.open(sys.argv[1]).convert("RGB")
        src = src.resize((489, 800), Image.LANCZOS)
        src.save(os.path.join(TEX, "gui", "screamer.png"))

    sounds = {
        "mama_scream": scream(),
        "mama_roar": roar(),
        "mama_eat": chomp(),
        "mama_hurt": hurt(),
        "mama_death": death(),
        "screamer": screamer_snd(),
        "siren": siren(),
        "alarm": alarm(),
        "nuke_boom": boom(),
        "quake_rumble": rumble(),
        "tornado_wind": wind(),
        "blackhole_hum": hum(),
        "meteor_whoosh": whoosh(),
    }
    for k, v in sounds.items():
        write_ogg(k, v)
        print("sound", k, f"{len(v) / SR:.1f}s")


if __name__ == "__main__":
    main()
