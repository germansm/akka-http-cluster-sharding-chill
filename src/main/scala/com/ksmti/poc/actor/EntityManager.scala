/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.ksmti.poc.actor.Entity.{EntityAvailability, EntityReservation}
import com.ksmti.poc.actor.EntityManager.{
  AvailabilityRequest,
  ReservationRequest
}

trait Request

trait Response

object EntityManager {

  case class ReservationRequest(entityID: Long) extends Request

  case class AvailabilityRequest(entityID: Long) extends Request
}

class EntityManager(domain: String) extends Actor with ActorLogging {

  def this() = this("default")

  val entityRegion: ActorRef = ClusterSharding(context.system).start(
    typeName = "Entity",
    entityProps = Props[Entity],
    settings = ClusterShardingSettings(context.system),
    extractEntityId = {

      case AvailabilityRequest(id) ⇒
        (id.toString, EntityAvailability)

      case ReservationRequest(id) ⇒
        (id.toString, EntityReservation)
    },
    extractShardId = { _ ⇒
      domain
    }
  )

  override def receive: Receive = {
    case msg: Request ⇒
      entityRegion.forward(msg)
  }
}
