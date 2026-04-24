#!/usr/bin/env python3
"""Generate app icons for Scorekeeper (Android + iOS)"""

from PIL import Image, ImageDraw, ImageFont
import os

BASE = os.path.dirname(os.path.abspath(__file__))

def create_base_icon(size, transparent_bg=False):
    """Create the icon at a given size."""
    if transparent_bg:
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    else:
        img = Image.new("RGBA", (size, size), (26, 35, 126, 255))  # #1A237E navy

    draw = ImageDraw.Draw(img)

    # Draw a simple trophy shape using polygons
    s = size
    cx = s / 2

    # Scale factor
    f = s / 1024.0

    # Trophy cup body (trapezoid)
    cup_top_w = 420 * f
    cup_bot_w = 320 * f
    cup_top_y = 180 * f
    cup_bot_y = 560 * f

    cup_left_top = cx - cup_top_w / 2
    cup_right_top = cx + cup_top_w / 2
    cup_left_bot = cx - cup_bot_w / 2
    cup_right_bot = cx + cup_bot_w / 2

    cup_poly = [
        (cup_left_top, cup_top_y),
        (cup_right_top, cup_top_y),
        (cup_right_bot, cup_bot_y),
        (cup_left_bot, cup_bot_y),
    ]
    draw.polygon(cup_poly, fill=(255, 255, 255, 255))

    # Handles (left and right arcs via ellipse cutouts approach - draw rounded rects)
    handle_w = 80 * f
    handle_h = 160 * f
    handle_y_top = cup_top_y + 40 * f
    handle_y_bot = handle_y_top + handle_h

    # Left handle
    lh_x0 = cup_left_top - handle_w
    lh_x1 = cup_left_top + 10 * f
    draw.ellipse([lh_x0, handle_y_top, lh_x1, handle_y_bot], outline=(255,255,255,255), width=int(28*f))

    # Right handle
    rh_x0 = cup_right_top - 10 * f
    rh_x1 = cup_right_top + handle_w
    draw.ellipse([rh_x0, handle_y_top, rh_x1, handle_y_bot], outline=(255,255,255,255), width=int(28*f))

    # Stem (rectangle below cup)
    stem_w = 80 * f
    stem_top = cup_bot_y
    stem_bot = cup_bot_y + 130 * f
    draw.rectangle([cx - stem_w/2, stem_top, cx + stem_w/2, stem_bot], fill=(255,255,255,255))

    # Base platform
    base_w = 380 * f
    base_h = 60 * f
    base_top = stem_bot
    base_bot = base_top + base_h
    draw.rectangle([cx - base_w/2, base_top, cx + base_w/2, base_bot], fill=(255,255,255,255))

    # Star on cup
    import math
    star_cx = cx
    star_cy = (cup_top_y + cup_bot_y) / 2 - 20 * f
    star_r_outer = 80 * f
    star_r_inner = 36 * f
    star_points = []
    for i in range(10):
        angle = math.pi / 2 + i * math.pi / 5  # start from top
        r = star_r_outer if i % 2 == 0 else star_r_inner
        star_points.append((star_cx + r * math.cos(angle), star_cy - r * math.sin(angle)))
    bg_color = (26, 35, 126, 255) if not transparent_bg else (0, 0, 0, 0)
    draw.polygon(star_points, fill=bg_color)

    return img


def save(img, path, size):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    resized = img.resize((size, size), Image.LANCZOS)
    resized.save(path, "PNG")
    print(f"  Saved {size}x{size} -> {path}")


# Generate base 1024 icon
base = create_base_icon(1024)
base_transparent = create_base_icon(432, transparent_bg=True)

ANDROID = os.path.join(BASE, "composeApp/src/androidMain/res")
android_sizes = {
    "mipmap-mdpi/ic_launcher.png": 48,
    "mipmap-hdpi/ic_launcher.png": 72,
    "mipmap-xhdpi/ic_launcher.png": 96,
    "mipmap-xxhdpi/ic_launcher.png": 144,
    "mipmap-xxxhdpi/ic_launcher.png": 192,
}

print("Generating Android icons...")
for rel, size in android_sizes.items():
    save(base, os.path.join(ANDROID, rel), size)

# Foreground adaptive icon (transparent bg, 432x432)
fg_path = os.path.join(ANDROID, "mipmap-xxxhdpi/ic_launcher_foreground.png")
os.makedirs(os.path.dirname(fg_path), exist_ok=True)
base_transparent.save(fg_path, "PNG")
print(f"  Saved 432x432 -> {fg_path}")

# iOS - single 1024x1024 (modern Xcode 15+ universal icon)
IOS = os.path.join(BASE, "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
print("\nGenerating iOS icon...")
save(base, os.path.join(IOS, "app-icon-1024.png"), 1024)

print("\nDone!")
