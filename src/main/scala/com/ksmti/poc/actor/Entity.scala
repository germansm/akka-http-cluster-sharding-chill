/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging}
import com.ksmti.poc.actor.Entity.{
  EntityAvailability,
  EntityReservation,
  SuccessOperation,
  SuccessReservation
}

case class EntityState(quantity: Long) {
  def add(quantity: Long): EntityState = {
    copy(quantity = quantity + this.quantity)
  }
}

object EmptyState extends EntityState(0L)

object Entity {

  object EntityReservation extends Request

  object EntityAvailability extends Request

  case class SuccessOperation(quantity: Long) extends Response

  object SuccessReservation extends SuccessOperation(1L)
}

class Entity extends Actor with ActorLogging {

  var state: EntityState = EmptyState

  override def receive: Receive = {
    case EntityReservation ⇒
      state = state.add(1)
      sender() ! SuccessReservation
    case EntityAvailability ⇒
      sender() ! SuccessOperation(state.quantity)
  }
}
