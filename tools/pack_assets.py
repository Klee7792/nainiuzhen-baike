#!/usr/bin/env python3
"""pack_assets.py — 把游戏素材打成单个 assets.pack 容器（奶牛镇百科专用）

格式（与 Kotlin 解码器 PackDecoder.kt 严格对应）：
  Header 16B:  magic "NZPK"(4) | version(1)=2 | flags(1)=0 | reserved(2) |
               entryCount u32le | indexOffset u32le
  Index  32B*N: nameHash u64le | blobOff u64le | compSize u32le |
                rawSize u32le | flags u16le(bit0=zlib) | pad u16le
  Names: N × ( u16le nameLen | XOR(name utf-8, NAME_KEY) )  —— 顺序与 Index 一致
  Blob:  compSize 字节 = XOR( zlib deflate(raw,9), K )，K=4 字节小端 (nameHash 低 32 位)
  nameHash = FNV-1a 64(relpath utf-8, '/' 分隔)
布局: Header | Index | Names | Blobs（blobOff 为绝对偏移，Names 夹在 Index 与 Blobs 之间）
魔数 "NZPK" 非任何已知容器格式 → 一般工具/小白无法识别；条目名经 NAME_KEY 异或混淆，
blob 密钥由文件名哈希派生，直接翻包看到的全是乱码（防小白，非加密）。
v2 新增 Names 段：纯内存解码器需按路径取条目，哈希表无法反查文件名，故补混淆名字表。

用法（⚠️ 必须用 --include-from 传「git 跟踪清单」，**别直接拿素材仓当 --src**）:
  素材仓根目录下混着未跟踪的原始游戏配置（`3.1.4/`，275 个）、`一些参数.txt`，
  以及私钥 `debug.keystore`；不传清单时本脚本按 `rglob` 收全部文件，会把它们
  一起打进包（实测 123 条 → 401 条，且会把私钥公开分发）。正确包恒为 **123 条**。
  cd <assets 仓>
  git ls-files | grep -vE '^(assets[.]pack|debug[.]keystore|[.]github/)' > pack_list.txt
  python <baike 仓>/tools/pack_assets.py --src . --out assets.pack --include-from pack_list.txt
  python tools/pack_assets.py --src ... --out ... --peek config/item_database.json

  自检配方：用**改动前**的干净工作区走同一流程，产出的包 sha256 应与已提交的旧包
  逐字节相同（打包是确定性的）；再用 --peek 或自写解码器抽查即可。
"""
import argparse
import struct
import sys
import zlib
from pathlib import Path

MAGIC = b"NZPK"
VERSION = 2
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


# 名字表混淆密钥（两端必须一致）：固定盐的 FNV-1a64 低 32 位
NAME_KEY = key4(fnv1a64(b"NZPK-NAME-TABLE"))


def read_include_list(src: Path, list_path: Path) -> list[Path]:
    """读取「相对路径清单」（每行一个，即 `git ls-files` 的原样输出），返回源目录下的文件列表。

    ⚠️ 为什么需要它：素材仓根目录混着未跟踪的原始游戏配置（`3.1.4/`，275 个）、
    `一些参数.txt` 与私钥 `debug.keystore`，`rglob` 会把它们全部打进包
    （实测 123 条 → 401 条）。清单是唯一可靠的过滤手段。
    清单里若出现 `assets.pack`（输出文件）或 `.git` 下的路径，直接忽略。
    """
    rows: list[str] = []
    for raw_line in list_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        rows.append(line)
    if not rows:
        sys.exit(f"ERR: 清单文件 {list_path} 里没有任何路径")
    src_root = src.resolve()
    files: list[Path] = []
    missing: list[str] = []
    for rel in rows:
        candidate = src / rel.replace("\\", "/").lstrip("/")
        # 防越界：清单里的相对路径不得逃出源目录（绝对路径 / .. 一律拒绝）
        if not candidate.resolve().is_relative_to(src_root):
            sys.exit(f"ERR: 清单项越出源目录: {rel}")
        if not candidate.is_file():
            missing.append(rel)
            continue
        if candidate.name == "assets.pack" or ".git" in candidate.parts:
            continue
        files.append(candidate)
    if missing:
        sys.exit(
            f"ERR: 清单里有 {len(missing)} 个文件在源目录下不存在（清单是否过期？）:\n  "
            + "\n  ".join(missing[:20])
        )
    return files


def pack(src: Path, out: Path, include_from: Path | None = None) -> None:
    if include_from is not None:
        files = sorted(read_include_list(src, include_from))
    else:
        files = sorted(
            p for p in src.rglob("*")
            if p.is_file() and p.name != "assets.pack" and ".git" not in p.parts
        )
    if not files:
        sys.exit(f"ERR: 源目录 {src} 下没有素材文件")
    # 显式清单里若混进签名密钥，会随包公开分发 —— 只警告不拦（清单即明确意图）。
    keys = [p.name for p in files if p.suffix.lower() in (".keystore", ".jks", ".p12", ".pfx")]
    if keys:
        print(f"WARN: 清单包含签名密钥，会被打进包并公开分发: {keys}", file=sys.stderr)
    index = bytearray()          # 索引区（紧跟 header）
    names = bytearray()          # 名字表（紧跟索引；顺序与 index 一致）
    blobs = bytearray()          # 数据区（紧跟名字表）
    index_off = HEADER_SIZE
    # 先建名字表（长度与文件顺序固定，才能算出 blob 区起始偏移）
    for p in files:
        nb = p.relative_to(src).as_posix().encode("utf-8")
        names += struct.pack("<H", len(nb)) + xor(nb, NAME_KEY)
    blob_off = index_off + len(files) * INDEX_ENTRY_SIZE + len(names)
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
    out.write_bytes(header + bytes(index) + bytes(names) + bytes(blobs))
    print(f"entries={len(files)} names={len(names)}B raw={raw_total/1048576:.2f}MB "
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
    ap.add_argument(
        "--include-from",
        help="只打包该清单文件里列出的相对路径（每行一个，如 git ls-files 的输出）。"
             "⚠️ 强烈建议始终传它 —— 不传则按 rglob 收全部文件，会把未跟踪的原始游戏配置"
             "与私钥一起打进包。正确包为 123 条。",
    )
    a = ap.parse_args()
    if a.peek:
        peek(Path(a.out), a.peek)
    else:
        pack(Path(a.src), Path(a.out), Path(a.include_from) if a.include_from else None)


if __name__ == "__main__":
    main()
