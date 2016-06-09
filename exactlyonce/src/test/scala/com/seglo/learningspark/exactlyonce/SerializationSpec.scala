package com.seglo.learningspark.exactlyonce

import org.scalatest.{FreeSpec, ShouldMatchers}

class SerializationSpec extends FreeSpec with ShouldMatchers {
  "Domain events" - {
    "sould serialize to and from JSON" in {

      import Health._
      import org.json4s.native.Serialization.{read, write => swrite}

      val event = Event(1,1,"foo","client",Critical)
      val ser = swrite(event)
      println(ser.toString)
      val deser = read[Event](ser)
      println(deser)
      deser shouldEqual event
    }
  }
}
