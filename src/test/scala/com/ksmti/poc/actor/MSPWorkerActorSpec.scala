/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import com.ksmti.poc.PublicEventsDirectory
import com.ksmti.poc.actor.EventsManager.{ConsultProgram, EventsProgram}
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration._

class MSPWorkerActorSpec
    extends TestKit(ActorSystem("MSPWorkerActorSpec"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import system.dispatcher

  implicit val timeOut: Timeout = 3.second

  "Worker Actor " should {
    " consult the Program" in {
      val workerActor = system.actorOf(
        Props(
          new MSPWorkerActor(
            EventsProgram(
              PublicEventsDirectory.mspProgram.headOption
                .map(_._2.values.toSeq)
                .getOrElse(Seq.empty)
            )
          )
        )
      )

      (workerActor ? ConsultProgram).map {
        case scalaEvents: EventsProgram =>
          scalaEvents.program.foreach(e => system.log.debug(e.toString))
          succeed
        case _ =>
          fail()
      }
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
