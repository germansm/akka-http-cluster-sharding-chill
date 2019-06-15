/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging}
import com.ksmti.poc.PublicEventsDirectory
import com.ksmti.poc.actor.PublicEventEntity.{
  AvailableStock,
  InvalidReservation,
  InvalidEvent,
  PublicEventStock,
  SeatsReservation,
  SuccessReservation
}
import org.joda.time.DateTime

object PublicEventEntity {

  object ReservationResponse

  case class SeatsReservation(seats: Int = 1) extends RequestMessage {
    require(seats > 0)
  }

  object PublicEventStock extends RequestMessage

  case class AvailableStock(stock: Int, timeStamp: String)
      extends ResponseMessage

  case class SuccessReservation(reservationID: Long, timeStamp: String)
      extends ResponseMessage

  object InvalidEvent extends ResponseMessage

  object InvalidReservation extends ResponseMessage

}

class PublicEventEntity extends Actor with ActorLogging {

  private lazy val eventID: String = self.path.name
  protected lazy val domain: String = context.parent.path.name

  private lazy val initialStock: Int = PublicEventsDirectory.mspProgram
    .get(domain)
    .flatMap(_.get(eventID))
    .map(_.stock)
    .getOrElse(0)

  private var committed: Int = 0

  override def receive: Receive = {
    case _ ⇒
      sender() ! InvalidEvent
  }

  private lazy val eventBehavior: Receive = {

    case PublicEventStock ⇒
      sender() ! AvailableStock(initialStock - committed,
                                DateTime.now.toDateTimeISO.toString())

    case SeatsReservation(seats) if initialStock >= committed + seats ⇒
      committed += seats
      sender() ! SuccessReservation(DateTime.now().getMillis,
                                    DateTime.now.toDateTimeISO.toString())

    case _: SeatsReservation ⇒
      sender() ! InvalidReservation
  }

  override def preStart(): Unit = {
    if (initialStock > 0) {
      context.become(eventBehavior)
    }
    super.preStart()
  }
}
