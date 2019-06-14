/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.DateTime
import com.ksmti.poc.actor.EntityManager.{
  AvailabilityRequest,
  ReservationRequest
}

object MSP {
  val Ping = "PING"
  val Pong = "PONG"
}

class MSP(name: String) extends Actor with ActorLogging {

  val manager: ActorRef = context.actorOf(Props[EntityManager])

  override def receive: Receive = {
    case MSP.Ping ⇒
      log.info("MSP[{}] received Ping at [{}]", name, DateTime.now)
      sender() ! MSP.Pong

    case MSP.Pong ⇒
      log.info("MSP[{}] received Ping at [{}]", name, DateTime.now)
      sender() ! MSP.Ping

    case msg: ReservationRequest ⇒
      manager.forward(msg)

    case msg: AvailabilityRequest ⇒
      manager.forward(msg)
  }
}
