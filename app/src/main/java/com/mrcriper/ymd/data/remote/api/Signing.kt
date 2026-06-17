package com.mrcriper.ymd.data.remote.api

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 signing for Yandex Music API requests.
 *
 * Port of [py-ref/ymd/api.py::get_download_info] signature algorithm.
 * The signing scheme concatenates stringified parameter values (commas removed)
 * then HMAC-SHA256s with [DEFAULT_SIGN_KEY].
 */
object Signing {
    const val DEFAULT_SIGN_KEY: String = "p93jhgh689SBReK6ghtw62"

    fun sign(params: Map<String, Any?>): String {
        val joined = params.values
            .joinToString(separator = "") { it.toString() }
            .replace(",", "")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(DEFAULT_SIGN_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal(joined.toByteArray(Charsets.UTF_8))
        // py-ref trims trailing '=' via [:-1] on the b64 output
        val b64 = Base64.encodeToString(raw, Base64.NO_WRAP)
        return b64.trimEnd('=')
    }
}
