/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.web

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.ksmti.poc.actor.Entity.{SuccessOperation, SuccessReservation}
import com.ksmti.poc.actor.EntityManager.{
  AvailabilityRequest,
  ReservationRequest
}
import com.ksmti.poc.actor.MSP

import scala.util.Success

trait API {

  implicit val timeout: Timeout = Timeout(10L, TimeUnit.SECONDS)

  protected def router: akka.actor.ActorRef

  protected def routes: Route =
    path("ping") {
      get {
        onComplete(router ? MSP.Ping) {

          case Success(MSP.Ping) =>
            complete(StatusCodes.Forbidden)

          case Success(MSP.Pong) ⇒
            complete(MSP.Pong)

          case _ ⇒
            complete(StatusCodes.BadRequest)
        }
      }
    } ~
      path("pong") {
        get {
          complete(StatusCodes.Forbidden)
        }
      } ~
      path("reserve") {
        parameters("id".as[Long]) { id ⇒
          get {
            onComplete(router ? ReservationRequest(id)) {

              case Success(SuccessReservation) =>
                complete(StatusCodes.OK)

              case _ ⇒
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      } ~
      path("availability") {
        parameters("id".as[Long]) { id =>
          get {
            onComplete(router ? AvailabilityRequest(id)) {

              case Success(SuccessOperation(l)) =>
                complete(l.toString)

              case _ ⇒
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
}
