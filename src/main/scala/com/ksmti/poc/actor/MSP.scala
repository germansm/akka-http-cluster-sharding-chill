/*
 *  Copyright (C) 2015-2019 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.ksmti.poc.PublicEventsDirectory
import com.ksmti.poc.PublicEventsDirectory.PublicEvent
import com.ksmti.poc.actor.MSP.{
  ConsultProgram,
  EventsProgram,
  ReservationRequest,
  StockRequest
}
import com.ksmti.poc.actor.PublicEventEntity.{
  PublicEventStock,
  SeatsReservation
}
import org.joda.time.DateTime

trait RequestMessage

trait ResponseMessage

object MSP {

  object ConsultProgram extends RequestMessage

  case class EventsProgram(program: Seq[PublicEvent],
                           timeStamp: Option[String] = None)
      extends ResponseMessage {
    def stamp: EventsProgram = {
      copy(timeStamp = Some(DateTime.now().toDateTimeISO.toString()))
    }
  }

  case class StockRequest(event: String) extends RequestMessage

  case class ReservationRequest(entityID: String) extends RequestMessage
}

class MSPWorkerActor(program: EventsProgram) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.debug("Hey !!!")
    super.preStart()
  }

  override def postStop(): Unit = {
    log.debug("See ya !!!")
    super.postStop()
  }

  override def receive: Receive = {
    case ConsultProgram ⇒
      sender() ! program.stamp
      self ! PoisonPill
  }
}

class MSP(name: String) extends Actor with ActorLogging {

  private val domain = PublicEventsDirectory.idGenerator(name)

  private lazy val publicEvents =
    PublicEventsDirectory.mspProgram(domain)

  private val program = EventsProgram(publicEvents.values.toSeq)

  private def worker: ActorRef =
    context.actorOf(Props(new MSPWorkerActor(program)))

  private val sharding = ClusterSharding(context.system).start(
    typeName = "PublicEventEntity",
    entityProps = Props[PublicEventEntity],
    settings = ClusterShardingSettings(context.system),
    extractEntityId = {
      case StockRequest(id) ⇒
        (PublicEventsDirectory.idGenerator(id), PublicEventStock)

      case ReservationRequest(id) ⇒
        (PublicEventsDirectory.idGenerator(id), SeatsReservation)
    },
    extractShardId = { _ ⇒
      // see https://manuel.bernhardt.io/2018/02/26/tour-akka-cluster-cluster-sharding/
      domain
    }
  )

  override def receive: Receive = {
    case ConsultProgram ⇒
      log.info("MSP[{}] received a ConsultProgram Request at [{}]",
               name,
               DateTime.now)
      worker.forward(ConsultProgram)

    case msg: ReservationRequest ⇒
      sharding.forward(msg)

    case msg: StockRequest ⇒
      sharding.forward(msg)
  }
}
