/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.testkit.TestKit
import com.ksmti.poc.actor.EventsManager.{
  AvailableStock,
  InvalidEvent,
  InvalidReservation,
  PublicEventStock,
  SeatsReservation,
  SuccessReservation
}
import akka.pattern.gracefulStop

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import com.ksmti.poc.PublicEventsDirectory
import com.ksmti.poc.PublicEventsDirectory.PublicEvent
import org.scalatest.compatible.Assertion
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class PublicEventEntitySpec
    extends TestKit(ActorSystem("ScalaEventEntitySpec"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  import system.dispatcher

  implicit val timeOut: Timeout = 3.second

  private val stopActorRef: ActorRef => Future[Assertion] = { ref =>
    gracefulStop(ref, 3.second).map(_ => succeed).recover {
      case th =>
        fail(th)
    }
  }

  private def processResponse[T](responseFunction: PartialFunction[T, Future[Assertion]]): T => Future[Assertion] = {
    responseFunction.orElse({
      case whatever =>
        fail(new UnsupportedOperationException(whatever.toString))
    }: PartialFunction[T, Future[Assertion]])(_)
  }

  private lazy val event: PublicEvent =
    PublicEventsDirectory.mspProgram.headOption
      .flatMap(_._2.headOption.map(_._2))
      .getOrElse(PublicEvent("Undefined", "Undefined", "Undefined", 0, 0.0))

  private lazy val name = PublicEventsDirectory.mspProgram.headOption.map(_._1).getOrElse("Undefined")

  val idFor: String => String = e => s"$name${PublicEventsDirectory.separator}${PublicEventsDirectory.idGenerator(e)}"

  class PublicEventEntityT extends PublicEventEntity {
    override protected lazy val name: String =
      PublicEventsDirectory.mspProgram.headOption.map(_._1).getOrElse("Undefined")
  }

  "ScalaEventEntity " should {

    " InvalidScalaEvent " in {
      val entityActor = system.actorOf(Props(new PublicEventEntityT()))

      (entityActor ? PublicEventStock).flatMap {
        processResponse {
          case InvalidEvent =>
            stopActorRef(entityActor)
        }
      }
    }

    " ScalaEventStock " in {

      val entityActor =
        system.actorOf(Props(new PublicEventEntityT()), idFor(event.name))

      (entityActor ? PublicEventStock).flatMap {
        processResponse {
          case AvailableStock(stk, _) =>
            stk shouldBe event.stock
            stopActorRef(entityActor)
        }
      }
    }

    " SeatsReservation " in {
      val entityActor =
        system.actorOf(Props(new PublicEventEntityT()), idFor(event.name))

      (entityActor ? SeatsReservation(event.stock + 1)).map {
        case InvalidReservation =>
          succeed
        case whatever =>
          fail(new UnsupportedOperationException(whatever.toString))
      }

      (entityActor ? SeatsReservation(event.stock)).map {
        case _: SuccessReservation =>
          succeed
        case whatever =>
          fail(new UnsupportedOperationException(whatever.toString))
      }

      (entityActor ? SeatsReservation(1)).flatMap {
        processResponse {
          case InvalidReservation =>
            stopActorRef(entityActor)
        }
      }
    }

    " Serial Operations " in {
      val entityActor =
        system.actorOf(Props(new PublicEventEntityT()), idFor(event.name))

      def testReservation(attempts: Long, attempt: Long = 1): Future[Assertion] = {

        if (attempt <= attempts) {
          (entityActor ? SeatsReservation(1)).flatMap {
            processResponse {
              case _: SuccessReservation =>
                (entityActor ? PublicEventStock).flatMap {
                  processResponse {
                    case AvailableStock(stock, _) if event.stock - attempt == stock =>
                      testReservation(attempts, attempt + 1)
                  }
                }
            }
          }
        } else {
          (entityActor ? SeatsReservation(1)).flatMap {
            processResponse {
              case InvalidReservation =>
                succeed
            }
          }
        }
      }
      testReservation(event.stock).flatMap { _ =>
        stopActorRef(entityActor)
      }
    }

    " Parallel Operations " in {
      val entityActor =
        system.actorOf(Props(new PublicEventEntityT()), idFor(event.name))

      Future
        .traverse(1 to event.stock) { _ =>
          (entityActor ? SeatsReservation()).flatMap {
            case _: SuccessReservation =>
              (entityActor ? PublicEventStock).map {
                case AvailableStock(stk, _) if stk <= event.stock =>
                  succeed
              }
          }
        }
        .flatMap { response =>
          (entityActor ? PublicEventStock).flatMap {
            case _ if response.size == event.stock =>
              stopActorRef(entityActor)
          }
        }
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
