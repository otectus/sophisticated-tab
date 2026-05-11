"""
Generates assets/sophisticatedtab/textures/gui/backpack_tab.png.

Approach: take LegendaryTabs' Inventory tab verbatim (rounded top corners,
beveled body, flat bottom — the exact LT chrome shape) and overwrite the
16x16 icon rectangle at offset (5, 3) with the chrome's body-fill color. The
ItemStack will render at that same position, so the entire icon area is
guaranteed to be hidden by the item and never seen.

Layout: 52x22 atlas — normal chrome at U=0, hover/active chrome at U=26.
"""
from PIL import Image


# LT lays each tab pair as (normal, hover) at +54. The Inventory tab is the
# best template because its chrome is canonical and (since LegendaryTabs is a
# tabs library) is presumably the reference shape for the project.
INVENTORY_TAB_NORMAL = (0, 0)
INVENTORY_TAB_HOVER = (54, 0)

TAB_W, TAB_H = 26, 22

# The 16x16 ItemStack always renders at this offset within the tab. We mask
# the same rectangle in the chrome so the icon area is uniform body fill and
# the player's real item sits cleanly on top.
ICON_X, ICON_Y, ICON_W, ICON_H = 5, 3, 16, 16


def body_fill_color(atlas, template_uv):
    """Sample the body fill color from the gutter between the left bevel and
    the icon (cols 2-4, rows 2-19). This area is body fill in every LT tab
    we've inspected — LT's icons stay within the 5..20 / 3..18 rectangle.

    We pick the MOST FREQUENT color in this gutter and require it to be a
    mid-luminance value (excludes the top-row highlight at row 1 and the
    border at row 0)."""
    from collections import Counter

    u0, v0 = template_uv
    counts = Counter()
    for y in range(2, TAB_H - 2):
        for x in range(2, 5):
            counts[atlas.getpixel((u0 + x, v0 + y))] += 1

    def luminance(rgba):
        r, g, b, _a = rgba
        return (r + g + b) / 3

    candidates = sorted(counts.items(), key=lambda kv: -kv[1])
    for color, _ in candidates:
        lum = luminance(color)
        if 80 < lum < 230:
            return color
    return candidates[0][0]


def build_chrome(atlas, template_uv):
    """Crop the template tab, then overwrite the 16x16 icon rectangle with
    body-fill color so we're left with chrome only."""
    u0, v0 = template_uv
    chrome = atlas.crop((u0, v0, u0 + TAB_W, v0 + TAB_H)).copy()
    body = body_fill_color(atlas, template_uv)
    print(f"  Template at {template_uv}, body fill {body}")
    for dy in range(ICON_H):
        for dx in range(ICON_W):
            chrome.putpixel((ICON_X + dx, ICON_Y + dy), body)
    return chrome


def main():
    atlas = Image.open(".tools/lt_atlas.png").convert("RGBA")

    print("Building normal chrome from Inventory tab template:")
    normal_chrome = build_chrome(atlas, INVENTORY_TAB_NORMAL)
    print("Building hover chrome from Inventory tab template:")
    hover_chrome = build_chrome(atlas, INVENTORY_TAB_HOVER)

    out = Image.new("RGBA", (TAB_W * 2, TAB_H), (0, 0, 0, 0))
    out.paste(normal_chrome, (0, 0))
    out.paste(hover_chrome, (TAB_W, 0))

    import os
    target = os.path.join(
        os.path.dirname(__file__),
        "..",
        "src",
        "main",
        "resources",
        "assets",
        "sophisticatedtab",
        "textures",
        "gui",
        "backpack_tab.png",
    )
    os.makedirs(os.path.dirname(target), exist_ok=True)
    out.save(target, "PNG")
    print(f"Wrote {os.path.abspath(target)} ({out.size[0]}x{out.size[1]})")


if __name__ == "__main__":
    main()
