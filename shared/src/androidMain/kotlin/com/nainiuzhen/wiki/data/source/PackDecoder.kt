package com.nainiuzhen.wiki.data.source

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

/**
 * assets.pack（NZPK v2）纯内存解码器 —— 不落盘、不留解压文件：
 *
 * - 冷启动后首次经 [get] 取条目时，才从 APK assets 读入 assets.pack 并解析
 *   Header / Index / Names 建索引（几毫秒级）；
 * - 之后每次取条目按需解码（XOR → zlib inflate）并缓存字节，整个包解完最多占
 *   raw ≈ 9MB 内存（进程被杀即释放，后台切回不重复解码）；
 * - 包缺失（如本地 debug 构建未注入 pack）时 [get] 返回 null，
 *   由调用方 [readAssetBytes] 回退到原生 assets 读取，行为与旧版一致。
 *
 * 格式（与 tools/pack_assets.py 严格对应，改动任一侧必须同步另一侧）：
 *   Header 16B: magic "NZPK" | version(1)=2 | flags(1) | reserved(2) |
 *               entryCount u32le | indexOffset u32le
 *   Index 32B*N: nameHash u64le | blobOff u64le | compSize u32le |
 *                rawSize u32le | flags u16le(bit0=zlib) | pad u16le
 *   Names: N × ( u16le nameLen | XOR(name utf-8, NAME_KEY) )，顺序与 Index 一致
 *   Blob: XOR( zlib deflate(raw,9) 或原样, K )，K = nameHash 低 32 位小端 4 字节
 *
 * 注：条目寻址直接用 Names 段还原的明文路径建映射（路径由调用方传入），
 * Kotlin 侧无需实现 FNV；nameHash 仅用于派生 blob 密钥。
 */
object PackDecoder {
    private const val EXPECTED_VERSION = 2

    /** 名字表混淆密钥盐，必须与 tools/pack_assets.py 的 NAME_KEY 盐一致。 */
    private const val NAME_KEY_SALT = "NZPK-NAME-TABLE"

    /** 名字表混淆密钥：盐的 FNV-1a64 低 32 位小端 4 字节。 */
    private val nameKey: ByteArray by lazy {
        key4(fnv1a64(NAME_KEY_SALT.encodeToByteArray()))
    }

    private class Entry(
        val name: String,
        val nameHash: Long,
        val blobOff: Int,
        val compSize: Int,
        val rawSize: Int,
        val zipped: Boolean,
    )

    private val lock = Any()

    /** 空 ByteArray = 包已加载但不存在（区分「未加载」与「缺失」）。 */
    @Volatile
    private var packBytes: ByteArray? = null

    @Volatile
    private var entriesByName: Map<String, Entry> = emptyMap()

    /** 已解码条目缓存（上限即全包 raw 总量 ≈ 9MB，进程退出即释放）。 */
    private val cache = HashMap<String, ByteArray>()

    /** 包内条目数（诊断用；未加载/缺失时为 0）。 */
    val entryCount: Int get() = entriesByName.size

    /**
     * 取 `assets.pack` 中 `path` 对应的字节；包缺失或无此条目返回 null。
     * 结果带缓存，重复取同一路径零开销。
     */
    fun get(path: String): ByteArray? {
        ensureLoaded()
        val pack = packBytes?.takeIf { it.isNotEmpty() } ?: return null
        synchronized(cache) { cache[path]?.let { return it } }
        val entry = entriesByName[path] ?: return null
        val bytes = decode(pack, entry)
        synchronized(cache) { cache[path] = bytes }
        return bytes
    }

    /** 首次访问时读入并解析包；此后直接命中内存索引。 */
    private fun ensureLoaded() {
        if (packBytes != null) return
        synchronized(lock) {
            if (packBytes != null) return
            val bytes = try {
                AppContextHolder.current.assets.open("assets.pack").use { it.readBytes() }
            } catch (_: Exception) {
                // 包不存在（未注入 pack 的构建）→ 置空标记并放行 assets 回退
                packBytes = ByteArray(0)
                return
            }
            entriesByName = parse(bytes)
            packBytes = bytes
        }
    }

    /** 解析 Header + Index + Names，构建 路径 → 条目 映射。 */
    private fun parse(pack: ByteArray): Map<String, Entry> {
        val buf = ByteBuffer.wrap(pack).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(4)
        buf.get(magic)
        check(magic.decodeToString() == "NZPK") { "assets.pack 魔数不符" }
        val version = buf.get().toInt()
        check(version == EXPECTED_VERSION) { "assets.pack 版本 $version != $EXPECTED_VERSION" }
        buf.get() // flags
        buf.short // reserved
        val count = buf.int
        val indexOff = buf.int

        val entries = ArrayList<Entry>(count)
        var nameOff = indexOff + count * 32
        for (i in 0 until count) {
            val eOff = indexOff + i * 32
            val nameHash = buf.getLong(eOff)
            val blobOff = buf.getLong(eOff + 8)
            val compSize = buf.getInt(eOff + 16)
            val rawSize = buf.getInt(eOff + 20)
            val flags = buf.getShort(eOff + 24).toInt()
            val nameLen = (pack[nameOff].toInt() and 0xFF) or ((pack[nameOff + 1].toInt() and 0xFF) shl 8)
            val nameBytes = ByteArray(nameLen) { j ->
                (pack[nameOff + 2 + j].toInt() xor nameKey[j % 4].toInt()).toByte()
            }
            entries += Entry(
                name = nameBytes.decodeToString(),
                nameHash = nameHash,
                blobOff = blobOff.toInt(),
                compSize = compSize,
                rawSize = rawSize,
                zipped = flags and 1 != 0,
            )
            nameOff += 2 + nameLen
        }
        return entries.associateBy { it.name }
    }

    /** XOR 解扰 + 按需 zlib inflate。 */
    private fun decode(pack: ByteArray, e: Entry): ByteArray {
        val blobKey = key4(e.nameHash and 0xFFFFFFFFL)
        val off = e.blobOff
        val payload = ByteArray(e.compSize)
        for (i in 0 until e.compSize) {
            payload[i] = (pack[off + i].toInt() xor blobKey[i % 4].toInt()).toByte()
        }
        if (!e.zipped) return payload
        val inflater = Inflater()
        inflater.setInput(payload)
        val out = ByteArray(e.rawSize)
        var total = 0
        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(out, total, out.size - total)
                if (n == 0 && inflater.needsInput()) error("assets.pack 条目数据截断: ${e.name}")
                total += n
            }
        } finally {
            inflater.end()
        }
        check(total == e.rawSize) { "assets.pack 条目解压尺寸不符: ${e.name} $total != ${e.rawSize}" }
        return out
    }

    // ---- 与 tools/pack_assets.py 相同的哈希实现（仅供 nameKey 派生）----

    private fun fnv1a64(data: ByteArray): Long {
        // FNV_OFFSET_BASIS 0xCBF29CE484222325 的 Long 两补码字面量；乘法溢出即 mod 2^64
        var h = -0x340d631b7bdddcdbL
        for (b in data) {
            h = (h xor (b.toLong() and 0xFF)) * 0x100000001b3L
        }
        return h
    }

    private fun key4(low32: Long): ByteArray {
        val k = low32.toInt()
        return byteArrayOf(
            (k and 0xFF).toByte(),
            ((k ushr 8) and 0xFF).toByte(),
            ((k ushr 16) and 0xFF).toByte(),
            ((k ushr 24) and 0xFF).toByte(),
        )
    }
}
