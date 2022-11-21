/*
 *  Copyright (C) 2015-2022 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc

import com.typesafe.config.{ Config, ConfigFactory }

import java.net.URLDecoder
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{ Failure, Success, Try }

object PublicEventsDirectory {

  type MSPInstance = String

  type PublicEventID = String

  case class PublicEvent(name: String, location: String, date: String, stock: Int, price: Double) {

    def this(config: Config) =
      this(
        config.getString("name"),
        config.getString("location"),
        config.getString("date"),
        config.getInt("stock"),
        config.getDouble("price"))
  }

  lazy val mspProgram: Map[MSPInstance, Map[PublicEventID, PublicEvent]] = {
    Try {
      val config: Config = ConfigFactory.load("program.conf")
      config.getObjectList("MSPEngine.program").asScala.map { obj =>
        val innerConfig = obj.toConfig
        (
          idGenerator(innerConfig.getString("msp")),
          innerConfig
            .getObjectList("publicEvents")
            .asScala
            .map { c =>
              val event = new PublicEvent(c.toConfig)
              (idGenerator(event.name), event)
            }
            .toMap)
      }
    } match {
      case Success(events) =>
        events.toMap
      case Failure(th) =>
        th.printStackTrace()
        Map.empty
    }
  }

  val split: String => Array[String] = { base =>
    URLDecoder.decode(base, "UTF-8").split(separator) :+ "Undefined"
  }

  lazy val separator: String = "::"

  val idGenerator: String => String = _.hashCode.toString

  val idGeneratorFor: (String, String) => String = (name, event) =>
    s"${idGenerator(name)}$separator${idGenerator(event)}"
}
