/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.http

import java.util.concurrent.TimeUnit

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.pattern.ask
import akka.util.Timeout
import com.ksmti.poc.actor.PublicEventEntity.{
  AvailableStock,
  InvalidEvent,
  InvalidReservation,
  SuccessReservation
}
import com.ksmti.poc.actor.MSP.{
  ConsultProgram,
  EventsProgram,
  ReservationRequest,
  StockRequest
}

import scala.util.{Failure, Success}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.ksmti.poc.PublicEventsDirectory.PublicEvent
import com.ksmti.poc.actor.Router.MSPNotFound

trait MessageProtocols extends DefaultJsonProtocol {
  implicit val scalaEventFormat: RootJsonFormat[PublicEvent] = jsonFormat5(
    PublicEvent)

  implicit val scalaEventsFormat: RootJsonFormat[EventsProgram] = jsonFormat2(
    EventsProgram)

  implicit val successReservationFormat: RootJsonFormat[SuccessReservation] =
    jsonFormat2(SuccessReservation)

  implicit val availableStockFormat: RootJsonFormat[AvailableStock] =
    jsonFormat2(AvailableStock)
}

trait API extends MessageProtocols {

  type ResponseFunction = PartialFunction[Any, StandardRoute]

  protected def log: LoggingAdapter

  implicit val timeout: Timeout = Timeout(10L, TimeUnit.SECONDS)

  protected def router: akka.actor.ActorRef

  private val commonResponse: ResponseFunction ⇒ ResponseFunction = {
    _.orElse({

      case MSPNotFound ⇒
        complete(StatusCodes.Gone)

      case InvalidReservation ⇒
        complete(StatusCodes.EnhanceYourCalm)

      case InvalidEvent ⇒
        complete(StatusCodes.ImATeapot)

      case whatever ⇒
        log.warning("Unexpected response [{}]", whatever)
        complete(StatusCodes.InternalServerError)
    })
  }

  private def requestAndThen[T](command: T)(
      responseFunction: ⇒ ResponseFunction): Route = {
    onComplete(router ? command) {
      case Success(result) ⇒
        commonResponse(responseFunction)(result)
      case Failure(th) ⇒
        th.printStackTrace()
        complete(StatusCodes.InternalServerError)
    }
  }

  protected def routes: Route =
    path(Segment / "upcomingEvents") { msp ⇒
      get {
        requestAndThen(ConsultProgram(msp)) {
          case events: EventsProgram =>
            complete(events)
        }
      }
    } ~
      path(Segment / "ticketsStock") { msp ⇒
        parameters("event") { event ⇒
          get {
            requestAndThen(StockRequest(msp, event)) {
              case stock: AvailableStock ⇒
                complete(stock)
            }
          }
        }
      } ~
      path(Segment / "reserveTickets") { msp ⇒
        parameters("event", "seats".as[Int] ? 1) { (id, seats) ⇒
          post {
            requestAndThen(ReservationRequest(msp, id, seats)) {
              case resp: SuccessReservation ⇒
                complete(resp)
            }
          }
        }
      }
}
