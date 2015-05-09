package nodescala

import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Created by michal on 5/4/15.
 */
object PlayGround extends App {

  val timeOut: Future[String] = Future.delay(2 seconds) continueWith {
    f => "Server timeout!"
  }

  val delayError: Future[String] = Future.delay(2 seconds) continueWith {
    f => {throw new Exception("Upss"); "Server timeout!"}
  }

  val test = for {
    t <- timeOut
    e <- timeOut
  } yield (t,e)

  test onComplete {
    case Success((t,e)) => println(s"Success $t $e")
    case Failure(e) => println(s"Failure $e")
    case o => println(s"other: $o")
  }

  Await.ready(test, Duration.Inf)
}
