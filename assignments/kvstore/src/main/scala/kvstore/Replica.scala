package kvstore

import akka.actor.{Actor, ActorRef, Props, SupervisorStrategy}
import akka.event.{Logging, LoggingReceive}
import kvstore.Arbiter._

import scala.concurrent.duration._
import scala.language.postfixOps

object Replica {

  sealed trait Operation {
    def key: String

    def id: Long
  }

  case class Insert(key: String, value: String, id: Long) extends Operation

  case class Remove(key: String, id: Long) extends Operation

  case class Get(key: String, id: Long) extends Operation

  sealed trait OperationReply

  case class OperationAck(id: Long) extends OperationReply

  case class OperationFailed(id: Long) extends OperationReply

  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  case object RetryPersist

  case class PersistedTimeout(peristnace: ActorRef)

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(new Replica(arbiter, persistenceProps))
}

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor {

  import Persistence._
  import Replica._
  import Replicator._
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */

  var kv = Map.empty[String, String]
  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]

  var expectedReplicaSeq: Long = 0

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  var pendingPersistenceOps = Map.empty[Long, (ActorRef, Persist)]
  val persistence = context.actorOf(persistenceProps)

  context.system.scheduler.schedule(100 millis, 100 millis, context.self, RetryPersist)

  val log = Logging(context.system, this)


  arbiter ! Join


  def receive = LoggingReceive {
    case JoinedPrimary => context.become(leader)
    case JoinedSecondary => context.become(replica)
  }

  /* TODO Behavior for  the leader role. */
  val leader: Receive = LoggingReceive {
    case Insert(key, value, id) =>
      kv += (key -> value)
      sender ! OperationAck(id)
    case Remove(key, id) =>
      kv -= key
      sender ! OperationAck(id)
    case Get(key, id) =>
      sender ! GetResult(key, kv get key, id)
  }

  /* TODO Behavior for the replica role. */
  val replica: Receive = LoggingReceive {
    case Get(key, id) =>
      sender ! GetResult(key, kv get key, id)
    case Snapshot(key, valueOption, seq) =>
      if (seq > expectedReplicaSeq) {

      } else if (seq < expectedReplicaSeq) {
        sender ! SnapshotAck(key, seq)
      } else {
        valueOption match {
          case Some(value) => kv += (key -> value)
          case None => kv -= key
        }

        persist(seq, key, valueOption)


      }

    case Persisted(key, seq) =>
      log.debug("Persisted !!!!!!!!!!!!!!")
      pendingPersistenceOps.get(seq).foreach(e => {
        val (recRef, _) = e
        log.debug("send to recRef:" + recRef + ", msg: " + SnapshotAck(key, seq))
        recRef ! SnapshotAck(key, seq)
        expectedReplicaSeq += 1
      })
      pendingPersistenceOps -= seq

    case RetryPersist =>
      pendingPersistenceOps foreach (e => {
        val (_, (_, op)) = e
        persistence ! op
      })


  }

  def persist(id: Long, key: String, valueOption: Option[String]) = {
    val op = Persist(key, valueOption, id)
    pendingPersistenceOps += (id ->(sender, op))
    persistence ! op
  }

}

