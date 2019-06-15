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
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.ksmti.poc.actor.PublicEventEntity.{
  AvailableStock,
  InvalidReservation,
  InvalidEvent,
  SuccessReservation
}
import com.ksmti.poc.actor.MSP.{
  ConsultProgram,
  ReservationRequest,
  EventsProgram,
  StockRequest
}

import scala.util.Success
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.ksmti.poc.PublicEventsDirectory.PublicEvent

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

  protected def log: LoggingAdapter

  implicit val timeout: Timeout = Timeout(10L, TimeUnit.SECONDS)

  protected def router: akka.actor.ActorRef

  protected def routes: Route =
    path("upcomingEvents") {
      get {
        onComplete(router ? ConsultProgram) {

          case Success(events: EventsProgram) =>
            complete(events)

          case _ ⇒
            complete(StatusCodes.InternalServerError)
        }
      }
    } ~
      path("ticketsStock") {
        parameters("event") { event ⇒
          get {
            onComplete(router ? StockRequest(event)) {

              case Success(stock: AvailableStock) ⇒
                complete(stock)

              case Success(InvalidReservation) ⇒
                complete(StatusCodes.EnhanceYourCalm)

              case Success(InvalidEvent) ⇒
                complete(StatusCodes.ImATeapot)

              case whatever ⇒
                log.warning("Unexpected response [{}]", whatever)
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      } ~
      path("reserve" / Segment) { id ⇒
        post {
          onComplete(router ? ReservationRequest(id)) {

            case Success(resp: SuccessReservation) ⇒
              complete(resp)

            case _ ⇒
              complete(StatusCodes.InternalServerError)
          }
        }
      }
}
