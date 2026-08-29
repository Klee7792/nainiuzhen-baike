package com.nainiuzhen.wiki.utils

import android.widget.Toast
import com.nainiuzhen.wiki.data.source.AppContextHolder

actual fun showToast(message: String) {
    Toast.makeText(AppContextHolder.current, message, Toast.LENGTH_SHORT).show()
}
