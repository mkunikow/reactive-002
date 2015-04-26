package week2

import frp.Var

class BankAccount {
  private var balance = 0;
  def deposit(amount: Int): Unit =
    if (amount > 0) balance = balance + amount;

  def withdraw(amount: Int): Int =
    if (0 < amount && amount <= balance) {
      balance = balance - amount;
      balance
    } else throw new Error("insufficient funds");
}

class BankAccountSignal {
  val balance = Var(0);
  def deposit(amount: Int): Unit =
    if (amount > 0) {
      val b = balance()
      balance() = b + amount
    }

  def withdraw(amount: Int): Unit =
    if (0 < amount && amount <= balance()) {
      val b = balance()
      balance() = b - amount;
    } else throw new Error("insufficient funds");
}
