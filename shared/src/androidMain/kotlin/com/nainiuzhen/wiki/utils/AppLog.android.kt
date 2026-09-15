package com.nainiuzhen.wiki.utils

import android.util.Log

/** @param line 已含级别与时间戳的一整行（见 [AppLog]）。 */
internal actual fun platformLog(line: String) {
    // E 级走 Log.e，其余走 Log.d。
    if (line.startsWith("[E ")) Log.e("Wiki", line) else Log.d("Wiki", line)
}
