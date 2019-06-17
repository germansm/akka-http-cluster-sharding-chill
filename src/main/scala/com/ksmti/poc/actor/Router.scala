/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.FromConfig
import com.ksmti.poc.actor.Router.MSPNotFound

trait RequestMessage

trait ResponseMessage

trait RoutedRequest extends RequestMessage {
  val msp: String
}

object Router {
  object MSPNotFound extends ResponseMessage
}

/**
  * Dummy Router Implementation
  */
class Router(hostname: String) extends Actor with ActorLogging {

  private val defaultMSP =
    context.system.settings.config.getString("MSPEngine.defaultMSP")

  private lazy val msp: ActorRef =
    context.actorOf(FromConfig.props(
                      Props(new MSP(defaultMSP))
                    ),
                    "MSP")

  // Dummy Impl. Routes to the MSPEngine.defaultMSP
  private def resolveMSP(instance: String): Option[ActorRef] = {
    if (defaultMSP == instance) {
      Some(msp)
    } else {
      None
    }
  }

  override def receive: Receive = {
    case request: RoutedRequest ⇒
      resolveMSP(request.msp).fold {
        sender() ! MSPNotFound
      }(_.forward(request))
  }
}
