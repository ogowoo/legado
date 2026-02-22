package io.dushu.app.help.storage

import cn.hutool.crypto.symmetric.AES
import io.dushu.app.help.config.LocalConfig
import io.dushu.app.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)