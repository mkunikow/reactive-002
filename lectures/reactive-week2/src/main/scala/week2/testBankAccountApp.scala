package week2

import frp.Signal

/**
 * Created by michal on 4/24/15.
 */
object testBankAccountApp extends App {
  def consolidated(accts: List[BankAccountSignal]): Signal[Int] =
    Signal(accts.map(_.balance()).sum)
  val a = new BankAccountSignal()
  val b = new BankAccountSignal()
  val c = consolidated(List(a,b))
  println(c())
  a deposit  20
  println(c())
  b deposit 30
  println(c())
  val exchange = Signal(246.00)
  val intDollar = Signal(c() * exchange())
  println(intDollar())
  b withdraw 10
  println(intDollar())

}
