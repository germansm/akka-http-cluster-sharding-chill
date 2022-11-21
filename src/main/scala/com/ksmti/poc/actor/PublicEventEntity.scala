/*
 *  Copyright (C) 2015-2022 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{ Actor, ActorLogging, Props }
import com.ksmti.poc.PublicEventsDirectory
import com.ksmti.poc.actor.EventsManager.{
  AvailableStock,
  InvalidEvent,
  InvalidReservation,
  PublicEventStock,
  SeatsReservation,
  SuccessReservation
}
import org.joda.time.DateTime

object PublicEventEntity {
  def props(): Props =
    Props(new PublicEventEntity())
}

class PublicEventEntity extends Actor with ActorLogging {

  protected lazy val name: String = PublicEventsDirectory.split(self.path.name)(0)

  private lazy val eventID: String = PublicEventsDirectory.split(self.path.name)(1)

  private lazy val initialStock: Int =
    PublicEventsDirectory.mspProgram.get(name).flatMap(_.get(eventID)).map(_.stock).getOrElse(0)

  private var committed: Int = 0

  override def receive: Receive = {
    case _ =>
      sender() ! InvalidEvent
  }

  private lazy val eventBehavior: Receive = {

    case PublicEventStock =>
      sender() ! AvailableStock(initialStock - committed, DateTime.now.toDateTimeISO.toString())

    case SeatsReservation(seats) if initialStock >= committed + seats =>
      committed += seats
      sender() ! SuccessReservation(DateTime.now().getMillis, DateTime.now.toDateTimeISO.toString())

    case _: SeatsReservation =>
      sender() ! InvalidReservation
  }

  override def preStart(): Unit = {
    log.info("PublicEventEntity preStart [{}] [{}] [{}]", name, eventID, DateTime.now)
    if (initialStock > 0) {
      context.become(eventBehavior)
    }
    super.preStart()
  }
}
