#!/usr/bin/env groovy

import java.security.MessageDigest

String call(String s, int limit = 0) {
    def x = MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
    if (limit) {
      return x.substring(0, limit)
    } else
      return x
}
