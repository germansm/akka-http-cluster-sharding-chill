/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.FromConfig

class Router(name: String) extends Actor with ActorLogging {

  val msp: ActorRef =
    context.actorOf(FromConfig.props(Props(new MSP(name))), name = "MSP")

  override def receive: Receive = {
    case msg ⇒
      msp.forward(msg)
  }
}
