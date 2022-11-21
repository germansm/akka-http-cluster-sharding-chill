/*
 *  Copyright (C) 2015-2022 KSMTI
 *
 *  <http://www.ksmti.com>
 *
 */

package com.ksmti.poc.actor

import akka.actor.{ Actor, ActorLogging, ActorRef, PoisonPill, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.ksmti.poc.PublicEventsDirectory
import com.ksmti.poc.PublicEventsDirectory.PublicEvent
import com.ksmti.poc.actor.EventsManager.{
  ConsultProgram,
  EventsProgram,
  PublicEventStock,
  RequestMessage,
  ReservationRequest,
  RoutedRequest,
  SeatsReservation,
  StockRequest
}
import org.joda.time.DateTime

object EventsManager {

  sealed trait RequestMessage

  sealed trait ResponseMessage

  object ConsultProgram extends RequestMessage

  case class EventsProgram(program: Seq[PublicEvent], timeStamp: Option[String] = None) extends ResponseMessage {
    def stamp: EventsProgram = {
      copy(timeStamp = Some(DateTime.now().toDateTimeISO.toString()))
    }
  }

  trait RoutedRequest extends RequestMessage {
    def event: String
  }

  case class StockRequest(event: String) extends RoutedRequest

  case class ReservationRequest(event: String, seats: Int = 1) extends RoutedRequest

  case class SeatsReservation(seats: Int = 1) extends RequestMessage {
    require(seats > 0)
  }

  object PublicEventStock extends RequestMessage

  case class AvailableStock(stock: Int, timeStamp: String) extends ResponseMessage

  case class SuccessReservation(reservationID: Long, timeStamp: String) extends ResponseMessage

  object InvalidEvent extends ResponseMessage

  object InvalidReservation extends ResponseMessage
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
    case ConsultProgram =>
      sender() ! program.stamp
      self ! PoisonPill
  }
}

class EventsManager(name: String) extends Actor with ActorLogging {

  private lazy val publicEvents = PublicEventsDirectory.mspProgram(PublicEventsDirectory.idGenerator(name))

  private val program = EventsProgram(publicEvents.values.toSeq)

  private def worker: ActorRef =
    context.actorOf(Props(new MSPWorkerActor(program)))

  private val shardId: String => String = { uid =>
    (math.abs(uid.hashCode) % 5).toString // Maximum 5 shards
  }

  private val sharding = ClusterSharding(context.system).start(
    typeName = "PublicEventEntity",
    entityProps = PublicEventEntity.props(),
    settings = ClusterShardingSettings(context.system),
    extractEntityId = {
      case StockRequest(id) =>
        (PublicEventsDirectory.idGeneratorFor(name, id), PublicEventStock)

      case ReservationRequest(id, seats) =>
        (PublicEventsDirectory.idGeneratorFor(name, id), SeatsReservation(seats))
    },
    extractShardId = {
      case cmd: RoutedRequest =>
        shardId(cmd.event)

      case ShardRegion.StartEntity(id) =>
        shardId(id)

      case whatever =>
        log.error("EventsManager#extractShardId unexpected message [{}]", whatever)
        ???
    })

  override def receive: Receive = {
    case ConsultProgram =>
      log.info("MSP[{}] received a ConsultProgram Request at [{}]", name, DateTime.now)
      worker.forward(ConsultProgram)

    case msg: RequestMessage =>
      sharding.forward(msg)
  }
}
