/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import com.ksmti.poc.actor.Entity.{
  EntityAvailability,
  EntityReservation,
  SuccessOperation,
  SuccessReservation
}

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{AsyncWordSpecLike, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class EntitySpec
    extends TestKit(ActorSystem("EntitySpec"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import system.dispatcher

  implicit val timeOut: Timeout = 3.second

  "EntityActor " should {
    " reserve " in {
      val entityActor = system.actorOf(Props[Entity])
      (entityActor ? EntityReservation).map {
        case _: SuccessOperation ⇒
          succeed
        case _ ⇒
          fail()
      }
    }

    " availability " in {
      val entityActor = system.actorOf(Props[Entity])
      for {
        SuccessOperation(0L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(1L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(2L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(3L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(4L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(5L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(6L) <- entityActor ? EntityAvailability
        _ <- entityActor ? EntityReservation
        SuccessOperation(7L) <- entityActor ? EntityAvailability
      } yield succeed
    }
    " availability(2) " in {
      val entityActor = system.actorOf(Props[Entity])
      Future
        .traverse(1 to 1000000) { _ ⇒
          (entityActor ? EntityReservation).map {
            case SuccessReservation ⇒
              succeed
          }
        }
        .flatMap { response ⇒
          (entityActor ? EntityAvailability).map {
            case _ if response.size == 1000000L ⇒
              succeed
          }
        }
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
