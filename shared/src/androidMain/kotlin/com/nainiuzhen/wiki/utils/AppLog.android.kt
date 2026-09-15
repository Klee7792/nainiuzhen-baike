package com.nainiuzhen.wiki.utils

import android.util.Log

internal actual fun platformLog(level: String, msg: String) {
    if (level == "E") Log.e("Wiki", msg) else Log.d("Wiki", msg)
}
