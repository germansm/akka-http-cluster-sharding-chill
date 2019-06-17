/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import com.ksmti.poc.actor.{MSP, Router}
import com.ksmti.poc.http.API
import com.typesafe.config.{Config, ConfigFactory}

object MSPEngine {

  private lazy val rootFolder = System.getProperty("user.dir") + "/target/native"

  def main(args: Array[String]): Unit = {

    val config: Config = ConfigFactory.load()

    val (hostname, httpPort) = args.toList match {
      case ifc :: http :: Nil ⇒
        (ifc, http)

      case ifc :: Nil ⇒
        (ifc, config.getString("akka.http.server.default-http-port"))

      case _ ⇒
        (config.getString("akka.remote.artery.canonical.hostname"),
         config.getString("akka.http.server.default-http-port"))
    }

    val system = ActorSystem(
      "MSPEngine",
      ConfigFactory.parseString(s"""
          akka.management.http.hostname = "$hostname"
          akka.remote.artery.canonical.hostname = "$hostname"
          akka.cluster.metrics.native-library-extract-folder = "${rootFolder}_$hostname"
        """).withFallback(config)
    )

    system.actorOf(
      Props(new MSP(system.settings.config.getString("MSPEngine.defaultMSP"))),
      "MSP")

    if (httpPort != "0") {
      new HttpListener(hostname, httpPort.toInt)(system)
    }

    AkkaManagement(system).start()

    ClusterBootstrap(system).start()

    Cluster(system).registerOnMemberUp({
      system.log.info("Cluster Ready !")
    })
  }
}

class HttpListener(hostname: String, port: Int)(implicit system: ActorSystem)
    extends API {

  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

  Http()
    .bindAndHandle(routes, hostname, port)
    .foreach { binding ⇒
      system.log.info("Started Service using [{}]", binding)
    }(system.dispatcher)

  override protected lazy val router: ActorRef = {
    system.actorOf(Props(new Router(hostname)), "Router")
  }

  override protected lazy val log: LoggingAdapter = system.log
}
