/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.FromConfig

class Router(hostname: String) extends Actor with ActorLogging {

  private lazy val msp: ActorRef =
    context.actorOf(
      FromConfig.props(
        Props(new MSP(
          context.system.settings.config.getString("MSPEngine.defaultMSP")))),
      "MSP")

  override def receive: Receive = {
    case msg ⇒
      msp.forward(msg)
  }
}
