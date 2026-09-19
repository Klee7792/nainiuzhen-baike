package com.nainiuzhen.wiki.utils

import platform.posix.exit

actual fun exitApplication() {
    exit(0)
}
