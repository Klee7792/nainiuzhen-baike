package com.nainiuzhen.wiki.data.source

import kotlin.concurrent.Volatile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * assets.pack（NZPK v2）纯内存解码器 —— 通用实现（Android / iOS 共用，2026-09-15 iOS 化下沉）：
 *
 * - 冷启动后首次经 [get] 取条目时，才读入 assets.pack 并解析
 *   Header / Index / Names 建索引（几毫秒级）；
 * - 之后每次取条目按需解码（XOR → zlib inflate）并缓存字节，整个包解完最多占
 *   raw ≈ 9MB 内存（进程被杀即释放，后台切回不重复解码）；
 * - 包缺失（如本地 debug 构建未注入 pack）时 [get] 返回 null，
 *   由调用方 [readAssetBytes] 回退到平台原生资源读取，行为与旧版一致。
 *
 * 跨平台改造说明（与 androidMain 旧版逐字节行为一致）：
 * - `ByteBuffer` → 手写小端读取（[u16le]/[i32le]/[i64le]）；
 * - `java.util.zip.Inflater` → expect [zlibInflate]（Android 用 Inflater，iOS 用 platform.zlib）；
 * - pack 来源 → expect [loadPackBytes]（Android 读 APK assets，iOS 读 app bundle）；
 * - `synchronized`（JVM 专属）→ [Mutex]（无竞争时同步完成，外层调用本就运行在
 *   AssetLoader 的 runBlocking 里，语义与旧版一致）。
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

    private val mutex = Mutex()

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
        if (packBytes?.isNotEmpty() != true) return null
        return runBlocking {
            mutex.withLock {
                cache[path]?.let { return@withLock it }
                val entry = entriesByName[path] ?: return@withLock null
                val bytes = decode(packBytes!!, entry)
                cache[path] = bytes
                bytes
            }
        }
    }

    /** 首次访问时读入并解析包；此后直接命中内存索引。 */
    private fun ensureLoaded() {
        if (packBytes != null) return
        runBlocking {
            mutex.withLock {
                if (packBytes != null) return@withLock
                val bytes = loadPackBytes()
                if (bytes == null) {
                    // 包不存在（未注入 pack 的构建）→ 置空标记并放行平台资源回退
                    packBytes = ByteArray(0)
                } else {
                    entriesByName = parse(bytes)
                    packBytes = bytes
                }
            }
        }
    }

    /** 解析 Header + Index + Names，构建 路径 → 条目 映射。 */
    private fun parse(pack: ByteArray): Map<String, Entry> {
        val magic = pack.decodeToString(0, 4)
        check(magic == "NZPK") { "assets.pack 魔数不符" }
        val version = pack[4].toInt()
        check(version == EXPECTED_VERSION) { "assets.pack 版本 $version != $EXPECTED_VERSION" }
        // pack[5] = flags，pack[6..7] = reserved
        val count = i32le(pack, 8)
        val indexOff = i32le(pack, 12)

        val entries = ArrayList<Entry>(count)
        var nameOff = indexOff + count * 32
        for (i in 0 until count) {
            val eOff = indexOff + i * 32
            val nameHash = i64le(pack, eOff)
            val blobOff = i64le(pack, eOff + 8)
            val compSize = i32le(pack, eOff + 16)
            val rawSize = i32le(pack, eOff + 20)
            val flags = u16le(pack, eOff + 24)
            val nameLen = u16le(pack, nameOff)
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

    /** XOR 解扰 + 按需 zlib inflate（平台实现见 [zlibInflate]）。 */
    private fun decode(pack: ByteArray, e: Entry): ByteArray {
        val blobKey = key4(e.nameHash and 0xFFFFFFFFL)
        val off = e.blobOff
        val payload = ByteArray(e.compSize)
        for (i in 0 until e.compSize) {
            payload[i] = (pack[off + i].toInt() xor blobKey[i % 4].toInt()).toByte()
        }
        if (!e.zipped) return payload
        return zlibInflate(payload, e.rawSize)
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

    // ---- 手写小端读取（替代 ByteBuffer，字节级行为一致）----

    private fun u16le(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)

    private fun i32le(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)

    private fun i64le(b: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) {
            v = (v shl 8) or (b[off + i].toLong() and 0xFF)
        }
        return v
    }
}

/**
 * 平台资源读取：读入 `assets.pack` 原始字节；包缺失（未注入）返回 null。
 * Android 经原生 AssetManager（APK assets），iOS 经 app bundle。
 */
internal expect fun loadPackBytes(): ByteArray?

/**
 * 平台 zlib 解压：输入为 zlib 包装格式（与 tools/pack_assets.py 的 zlib.compress 对应），
 * 输出长度必须恰为 [rawSize]，不符时抛异常。
 */
internal expect fun zlibInflate(data: ByteArray, rawSize: Int): ByteArray
