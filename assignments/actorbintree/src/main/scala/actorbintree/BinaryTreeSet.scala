/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /**
   * Request with identifier `id` to insert an element `elem` into the tree.
   * The actor at reference `requester` should be notified when this operation
   * is completed.
   */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /**
   * Request with identifier `id` to check whether an element `elem` is present
   * in the tree. The actor at reference `requester` should be notified when
   * this operation is completed.
   */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /**
   * Request with identifier `id` to remove the element `elem` from the tree.
   * The actor at reference `requester` should be notified when this operation
   * is completed.
   */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /**
   * Holds the answer to the Contains request with identifier `id`.
   * `result` is true if and only if the element is present in the tree.
   */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}

class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case op: Operation => root ! op
    case GC => {
      val newRoot = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))
      context.become(garbageCollecting(newRoot))
      root ! CopyTo(newRoot)
    }
  }

  // optional
  /**
   * Handles messages while garbage collection is performed.
   * `newRoot` is the root of the new binary tree where we want to copy
   * all non-removed elements into.
   */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case op: Operation => pendingQueue :+= op
    case GC => None
    case CopyFinished => {

      context.stop(root)
      root = newRoot
      context.become(normal)
      for (message <- pendingQueue) newRoot ! message
      pendingQueue = Queue.empty[Operation]
    }
  }
}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved
  var notifyParent = false

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */

  val normal: Receive = {
    case op: Insert =>
      insertCmd(op)

    case op: Contains =>
      containsCmd(op)

    case op: Remove =>
      removeCmd(op)

    case op: CopyTo =>
      copyToCmd(op)

  }



  def copyToCmd(op: CopyTo) = {
    var expected = Set[ActorRef]()

    if (subtrees.contains(Right)) {
      expected += subtrees(Right)
      subtrees(Right) ! CopyTo(op.treeNode)
    }
    if (subtrees.contains(Left)) {
      expected += subtrees(Left)
      subtrees(Left) ! CopyTo(op.treeNode)
    }
    context.become(copying(expected, false))

    if (!removed) op.treeNode ! Insert(self, 0, elem)
    else self ! OperationFinished(0)
  }

  def containsCmd(op: Operation) = {
    if (op.elem == elem) op.requester ! ContainsResult(op.id, !removed)
    else if (op.elem > elem) {
      if (subtrees.contains(Right)) {
        subtrees(Right) ! op
      } else op.requester ! ContainsResult(op.id, false)
    } else {
      if (subtrees.contains(Left)) subtrees(Left) ! op
      else op.requester ! ContainsResult(op.id, false)
    }
  }

  def removeCmd(op: Operation) = {
    if (op.elem == elem) {
      removed = true
      op.requester ! OperationFinished(op.id)
    } else if (op.elem > elem) {
      if (subtrees.contains(Right)) subtrees(Right) ! op
      else op.requester ! OperationFinished(op.id)
    } else {
      if (subtrees.contains(Left)) subtrees(Left) ! op
      else op.requester ! OperationFinished(op.id)
    }
  }

  def insertCmd(op: Operation) = {
    if (op.elem == elem) {
      removed = false
      op.requester ! OperationFinished(op.id)
    } else if (op.elem > elem) {
      if (subtrees.contains(Right)) subtrees(Right) ! op
      else {
        subtrees += (Right -> context.actorOf(BinaryTreeNode.props(op.elem, false)))
        op.requester ! OperationFinished(op.id)
      }
    } else {
      if (subtrees.contains(Left)) subtrees(Left) ! op
      else {
        subtrees += (Left -> context.actorOf(BinaryTreeNode.props(op.elem, false)))
        op.requester ! OperationFinished(op.id)
      }
    }
  }


  // optional
  /**
   * `expected` is the set of ActorRefs whose replies we are waiting for,
   * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
   */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case CopyFinished => {
      val left = expected - sender
      if (left.isEmpty && insertConfirmed) {
        context.become(normal)
        context.parent ! CopyFinished
      } else
        context.become(copying(left, insertConfirmed))
    }
    case OperationFinished(_) => {
      if (expected.isEmpty) {
        context.become(normal)
        context.parent ! CopyFinished
      } else
        context.become(copying(expected, true))
    }
  }

}
