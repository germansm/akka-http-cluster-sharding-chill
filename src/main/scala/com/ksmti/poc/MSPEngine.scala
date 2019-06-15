/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc

import akka.actor.{Actor, ActorPath, ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import com.ksmti.poc.actor.MSP.{ReservationRequest, StockRequest}
import com.ksmti.poc.actor.PublicEventEntity.{
  PublicEventStock,
  SeatsReservation
}
import com.ksmti.poc.actor.{MSP, Router, PublicEventEntity}
import com.ksmti.poc.http.API
import com.typesafe.config.ConfigFactory

object MSPEngine {

  private lazy val rootFolder = System.getProperty("user.dir") + "/target/native"

  def main(args: Array[String]): Unit = {

    val hostname = args.toList match {
      case ifc :: Nil ⇒
        ifc
      case _ ⇒
        "127.0.0.1"
    }

    val system = ActorSystem(
      "MSPEngine",
      ConfigFactory.parseString(s"""
          akka.management.http.hostname = "$hostname"
          akka.remote.artery.canonical.hostname = "$hostname"
          akka.cluster.metrics.native-library-extract-folder = "${rootFolder}_$hostname"
        """).withFallback(ConfigFactory.load())
    )

    system.actorOf(
      Props(new MSP(system.settings.config.getString("MSPEngine.defaultMSP"))),
      "MSP")

    new HttpListener(hostname)(system)

    AkkaManagement(system).start()

    ClusterBootstrap(system).start()

    Cluster(system).registerOnMemberUp({
      system.log.info("Cluster Ready !")
    })
  }
}

class HttpListener(hostname: String)(implicit system: ActorSystem) extends API {

  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

  Http()
    .bindAndHandle(routes, hostname)
    .foreach { binding ⇒
      system.log.info("Started Service using [{}]", binding)
    }(system.dispatcher)

  override protected lazy val router: ActorRef = {
    system.actorOf(Props(new Router(hostname)), "Router")
  }

  override protected lazy val log: LoggingAdapter = system.log
}
