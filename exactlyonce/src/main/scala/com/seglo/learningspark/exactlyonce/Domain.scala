package com.seglo.learningspark.exactlyonce

import org.json4s.JsonAST.JInt
import org.json4s.{CustomSerializer, NoTypeHints}

case class PartitionAndOffset(partition: Int, offset: Long)

sealed trait Health { def code: Int }
object Health {
  case object Nominal extends Health {val code = 0}
  case object Warning extends Health {val code = 1}
  case object Critical extends Health {val code = 2}

  implicit val formats = org.json4s.native.Serialization.formats(NoTypeHints) + new HealthSerializer
  class HealthSerializer extends CustomSerializer[Health](format => ({
    case JInt(code) => code.intValue() match {
      case 0 => Nominal
      case 1 => Warning
      case 2 => Critical
      case _ => throw new IllegalArgumentException("Expected a valid health code.")
    }
  }, { case x: Health => JInt(x.code) }))
}

case class Event(id: Long, ts: Long, source: String, client: String, health: Health)
case class EventWithOffset(offset: PartitionAndOffset, event: Event)
case class Alert(ts: Long, source: String, eventCount: Int)
