import plistlib, glob, os, re
from collections import defaultdict
from PIL import Image, ImageDraw, ImageFont

RES = r'D:/1Project/nainiuzhen-wiki/nainiuzhen-baike/app/src/main/assets/res'
OUT = r'D:/1Project/nainiuzhen-wiki/nainiuzhen-baike/builds/tmp_scale_compare.png'
SCALES = [1, 2, 3, 4, 5, 6, 7, 8, 9]
CELL = 200          # 每格边长(px)，容纳最大 x5=100
LABEL_H = 26        # 顶部倍率标注行高
PAD = 10
MARGIN_L = 120      # 左侧留给素材名


def nums(s):
    return list(map(int, re.findall(r'-?\d+', s)))


def slice_frame(sheet_img, info):
    """按 Cocos2d 旧格式 plist 把一帧还原成独立 RGBA 图(带透明边距)。"""
    n = nums(info.get('frame'))
    x, y, w, h = n[0], n[1], n[2], n[3]
    crop = sheet_img.crop((x, y, x + w, y + h))
    if info.get('rotated', False):
        crop = crop.transpose(Image.ROTATE_90)
    n2 = nums(info.get('sourceSize'))
    sw, sh = n2[0], n2[1]
    n3 = nums(info.get('sourceColorRect'))
    cx, cy, cw, ch = n3[0], n3[1], n3[2], n3[3]
    canvas = Image.new('RGBA', (sw, sh), (0, 0, 0, 0))
    canvas.paste(crop, (cx, cy))
    return canvas


def pick_frames():
    want = {'20x20': 2, '22x22': 100, 'big': 2}
    got = defaultdict(int)
    chosen = []
    for p in sorted(glob.glob(os.path.join(RES, 'items*.plist'))):
        with open(p, 'rb') as f:
            pl = plistlib.load(f)
        sheet = os.path.splitext(os.path.basename(p))[0] + '.png'
        sheet_img = Image.open(os.path.join(RES, sheet)).convert('RGBA')
        for name, info in pl.get('frames', {}).items():
            if not isinstance(info, dict):
                continue
            if info.get('rotated', False):
                continue
            n2 = nums(info.get('sourceSize'))
            sw, sh = n2[0], n2[1]
            key = f'{sw}x{sh}'
            if key == '20x20' and got['20x20'] < want['20x20']:
                chosen.append((name, slice_frame(sheet_img, info)))
                got['20x20'] += 1
            elif key == '22x22' and got['22x22'] < want['22x22']:
                chosen.append((name, slice_frame(sheet_img, info)))
                got['22x22'] += 1
            elif (sw > 30 or sh > 30) and got['big'] < want['big']:
                chosen.append((name, slice_frame(sheet_img, info)))
                got['big'] += 1
        if all(got[k] >= want[k] for k in want):
            break
    return chosen


def main():
    chosen = pick_frames()
    cols, rows = len(SCALES), len(chosen)
    W = MARGIN_L + cols * CELL + PAD
    H = LABEL_H + rows * (CELL + LABEL_H) + PAD
    out = Image.new('RGBA', (W, H), (240, 240, 240, 255))
    d = ImageDraw.Draw(out)
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None

    for ci, sc in enumerate(SCALES):
        x0 = MARGIN_L + ci * CELL
        d.text((x0 + CELL // 2, LABEL_H // 2), f'x{sc}', fill=(0, 0, 0, 255), font=font, anchor='mm')

    for ri, (name, img) in enumerate(chosen):
        y0 = LABEL_H + PAD + ri * (CELL + LABEL_H)
        d.text((6, y0 + CELL // 2), name, fill=(150, 0, 0, 255), font=font, anchor='lm')
        for ci, sc in enumerate(SCALES):
            x0 = MARGIN_L + ci * CELL
            big = img.resize((img.width * sc, img.height * sc), Image.NEAREST)
            bx = x0 + (CELL - big.width) // 2
            by = y0 + (CELL - big.height) // 2
            d.rectangle([x0 + 2, y0 + 2, x0 + CELL - 2, y0 + CELL - 2], outline=(200, 200, 200, 255))
            out.paste(big, (bx, by), big)

    out.convert('RGB').save(OUT)
    print('OK', out.size, 'frames=', [n for n, _ in chosen])


if __name__ == '__main__':
    main()
