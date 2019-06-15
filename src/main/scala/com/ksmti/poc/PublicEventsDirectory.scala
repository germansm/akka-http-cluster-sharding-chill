/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success, Try}

import scala.collection.JavaConverters._

object PublicEventsDirectory {

  type MSPInstance = String

  type PublicEventID = String

  case class PublicEvent(name: String,
                         location: String,
                         date: String,
                         stock: Int,
                         price: Double) {

    def this(config: Config) = this(
      config.getString("name"),
      config.getString("location"),
      config.getString("date"),
      config.getInt("stock"),
      config.getDouble("price")
    )
  }

  lazy val mspProgram: Map[MSPInstance, Map[PublicEventID, PublicEvent]] = {
    Try {
      val config: Config = ConfigFactory.load("program.conf")
      config.getObjectList("MSPEngine.program").asScala map { obj ⇒
        val innerConfig = obj.toConfig
        (idGenerator(innerConfig.getString("msp")),
         innerConfig
           .getObjectList("publicEvents")
           .asScala
           .map { c ⇒
             val event = new PublicEvent(c.toConfig)
             (idGenerator(event.name), event)
           }
           .toMap)
      }
    } match {
      case Success(events) ⇒
        events.toMap
      case Failure(th) ⇒
        th.printStackTrace()
        Map.empty
    }
  }

  val idGenerator: String ⇒ String = _.hashCode.toString
}
