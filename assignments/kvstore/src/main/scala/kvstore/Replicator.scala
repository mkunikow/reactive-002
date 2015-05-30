package kvstore

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef
import akka.event.{Logging, LoggingReceive}
import akka.persistence.AtLeastOnceDelivery
import scala.concurrent.duration._

object Replicator {
  case class Replicate(key: String, valueOption: Option[String], id: Long)
  case class Replicated(key: String, id: Long)
  
  case class Snapshot(key: String, valueOption: Option[String], seq: Long)
  case class SnapshotAck(key: String, seq: Long)

  case object RetrySnapshot

  def props(replica: ActorRef): Props = Props(new Replicator(replica))
}

class Replicator(val replica: ActorRef) extends Actor {
  import Replicator._
  import Replica._
  import context.dispatcher
  
  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  // map from sequence number to pair of sender and request
  var acks = Map.empty[Long, (ActorRef, Replicate)]
  // a sequence of not-yet-sent snapshots (you can disregard this if not implementing batching)
  var pending = Vector.empty[Snapshot]
  
  var _seqCounter = 0L
  def nextSeq = {
    val ret = _seqCounter
    _seqCounter += 1
    ret
  }

  val log = Logging(context.system, this)

  context.system.scheduler.schedule(100.millis, 100.millis, context.self, RetrySnapshot)



  
  /* TODO Behavior for the Replicator. */
  def receive: Receive = LoggingReceive {
    case r @ Replicate(k,v,id) =>
      val seq = nextSeq
      acks += (seq -> (sender(), r))
      replica ! Snapshot(k, v, seq)
    case SnapshotAck(k,s) =>
      acks.get(s).foreach(a => {
        acks -= s
        a._1 ! Replicated(a._2.key, a._2.id)
      })
    case RetrySnapshot =>
      acks.foreach(a => {
        log.debug("RetrySnapshot resend: " + a)
        replica ! Snapshot(a._2._2.key, a._2._2.valueOption, a._1)
      })

  }

}
