package com.seglo.learningspark.exactlyonce

import com.typesafe.config.Config

object ConfigHelper {
  implicit class RichConfig(val underlying: Config) extends AnyVal {
    def getOptionalBoolean(path: String): Option[Boolean] = getByPath(path, underlying.getBoolean)

    def getOptionalString(path: String): Option[String] = getByPath(path, underlying.getString)

    def getOptionalInt(path: String): Option[Int] = getByPath(path, underlying.getInt)

    private def getByPath[T](path: String, f: String => T) = if (underlying.hasPath(path)) {
      Some(f(path))
    } else {
      None
    }
  }
}
