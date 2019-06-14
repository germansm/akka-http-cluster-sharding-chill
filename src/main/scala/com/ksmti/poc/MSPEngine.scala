/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import com.ksmti.poc.actor.{MSP, Router}
import com.ksmti.poc.web.API
import com.typesafe.config.ConfigFactory

object MSPEngine {

  private lazy val rootFolder = System.getProperty("user.dir") + "/target/native"

  def main(args: Array[String]): Unit = {

    val interface = args.toList match {
      case ifc :: Nil ⇒
        ifc
      case _ ⇒
        "127.0.0.1"
    }

    val system = ActorSystem(
      "MSPEngine",
      ConfigFactory.parseString(s"""
          akka.management.http.hostname = "$interface"
          akka.remote.artery.canonical.hostname = "$interface"
          akka.cluster.metrics.native-library-extract-folder = "${rootFolder}_$interface"
        """).withFallback(ConfigFactory.load())
    )

    system.actorOf(Props(new MSP(interface)), "MSP")

    new HttpListener(interface)(system)

    AkkaManagement(system).start()

    ClusterBootstrap(system).start()

    Cluster(system).registerOnMemberUp({
      system.log.info("Cluster Ready !")
    })

  }

}

class HttpListener(interface: String)(implicit system: ActorSystem)
    extends API {

  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

  Http()
    .bindAndHandle(routes, interface)
    .foreach { binding ⇒
      system.log.info("Started Service using [{}]", binding)
    }(system.dispatcher)

  override protected lazy val router: ActorRef = {
    system.actorOf(Props(new Router(interface)), "Router")
  }
}
