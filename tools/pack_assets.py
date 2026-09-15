#!/usr/bin/env python3
"""pack_assets.py — 把游戏素材打成单个 assets.pack 容器（奶牛镇百科专用）

格式（与 Kotlin 解码器 PackDecoder.kt 严格对应）：
  Header 16B:  magic "NZPK"(4) | version(1)=1 | flags(1)=0 | reserved(2) |
               entryCount u32le | indexOffset u32le
  Index  32B*N: nameHash u64le | blobOff u64le | compSize u32le |
                rawSize u32le | flags u16le(bit0=zlib) | pad u16le
  Blob:  compSize 字节 = XOR( zlib deflate(raw,9), K )，K=4 字节小端 (nameHash 低 32 位)
  nameHash = FNV-1a 64(relpath utf-8, '/' 分隔)
魔数 "NZPK" 非任何已知容器格式 → 一般工具/小白无法识别；XOR 密钥由文件名哈希派生，
条目名不出现在包内（哈希即改名），防直接翻看。

用法:
  python tools/pack_assets.py --src D:/1Project/nainiuzhen-assets --out D:/1Project/nainiuzhen-assets/assets.pack
  python tools/pack_assets.py --src ... --out ... --peek config/item_database.json
"""
import argparse
import struct
import sys
import zlib
from pathlib import Path

MAGIC = b"NZPK"
VERSION = 1
HEADER_SIZE = 16
INDEX_ENTRY_SIZE = 32
FNV_OFFSET = 0xCBF29CE484222325
FNV_PRIME = 0x100000001B3
MASK64 = (1 << 64) - 1


def fnv1a64(data: bytes) -> int:
    h = FNV_OFFSET
    for b in data:
        h ^= b
        h = (h * FNV_PRIME) & MASK64
    return h


def key4(name_hash: int) -> bytes:
    return struct.pack("<I", name_hash & 0xFFFFFFFF)


def xor(data: bytes, k: bytes) -> bytes:
    return bytes(b ^ k[i % 4] for i, b in enumerate(data))


def pack(src: Path, out: Path) -> None:
    files = sorted(
        p for p in src.rglob("*")
        if p.is_file() and p.name != "assets.pack" and ".git" not in p.parts
    )
    if not files:
        sys.exit(f"ERR: 源目录 {src} 下没有素材文件")
    index = bytearray()          # 索引区（紧跟 header）
    blobs = bytearray()          # 数据区（紧跟索引）
    index_off = HEADER_SIZE
    blob_off = index_off + len(files) * INDEX_ENTRY_SIZE
    offset = blob_off            # blobOff 存绝对偏移
    raw_total = 0
    for p in files:
        rel = p.relative_to(src).as_posix()
        nh = fnv1a64(rel.encode("utf-8"))
        raw = p.read_bytes()
        raw_total += len(raw)
        comp = zlib.compress(raw, 9)
        use_zlib = len(comp) < len(raw)
        payload = comp if use_zlib else raw
        enc = xor(payload, key4(nh))
        flags = 1 if use_zlib else 0
        index += struct.pack("<QQIIHH4x", nh, offset, len(enc), len(raw), flags, 0)
        blobs += enc
        offset += len(enc)
    header = struct.pack(
        "<4sBBHII", MAGIC, VERSION, 0, 0, len(files), index_off
    )
    out.write_bytes(header + bytes(index) + bytes(blobs))
    print(f"entries={len(files)} raw={raw_total/1048576:.2f}MB "
          f"pack={out.stat().st_size/1048576:.2f}MB -> {out}")


def peek(out: Path, rel: str) -> None:
    """按解码器同款逻辑取回一个条目，打印前 80 字节与大小，用于人工核对。"""
    data = out.read_bytes()
    magic, ver, _, _, count, idx_off = struct.unpack_from("<4sBBHII", data, 0)
    assert magic == MAGIC and ver == VERSION, "魔数/版本不对"
    target = fnv1a64(rel.encode("utf-8"))
    for i in range(count):
        nh, off, csize, rsize, flags, _ = struct.unpack_from(
            "<QQIIHH", data, idx_off + i * INDEX_ENTRY_SIZE
        )
        if nh == target:
            payload = xor(data[off:off + csize], key4(nh))
            raw = zlib.decompress(payload) if flags & 1 else payload
            assert len(raw) == rsize, "尺寸不符"
            print(f"[{rel}] raw={rsize}B comp={csize}B zlib={bool(flags & 1)}")
            print("head:", raw[:80])
            return
    sys.exit(f"ERR: 包里没有 {rel}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--peek")
    a = ap.parse_args()
    if a.peek:
        peek(Path(a.out), a.peek)
    else:
        pack(Path(a.src), Path(a.out))


if __name__ == "__main__":
    main()
