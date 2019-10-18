/* Copyright 2018 phData Inc. */

package io.phdata.provisioning

import io.circe.Encoder

sealed trait Message {
  val message: String
  val workspaceId: Long
}

case class ExceptionMessage(override val workspaceId: Long, override val message: String, exception: Throwable)
    extends Message

case class SimpleMessage(override val workspaceId: Long, override val message: String) extends Message

object Message {
  implicit final val encoder: Encoder[Message] =
    Encoder.forProduct1("message")(c => c.message)
}
