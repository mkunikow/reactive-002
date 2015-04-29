package calculator

sealed abstract class Expr
final case class Literal(v: Double) extends Expr
final case class Ref(name: String) extends Expr
final case class Plus(a: Expr, b: Expr) extends Expr
final case class Minus(a: Expr, b: Expr) extends Expr
final case class Times(a: Expr, b: Expr) extends Expr
final case class Divide(a: Expr, b: Expr) extends Expr

object Calculator {
  def computeValues(
      namedExpressions: Map[String, Signal[Expr]]): Map[String, Signal[Double]] = {
    (for (k <- namedExpressions.keys)
      yield k -> Signal(eval(getReferenceExpr(k, namedExpressions), namedExpressions))).toMap
  }

  def eval(expr: Expr, references: Map[String, Signal[Expr]]): Double = {
    def evalVisited(expr: Expr, references: Map[String, Signal[Expr]], visited: Set[String]): Double = {
      expr match {
        case Literal(v) => v
        case Ref(key) => if (!visited(key)) evalVisited(getReferenceExpr(key, references), references, visited + key) else Double.NaN
        case Plus(a,b) => evalVisited(a, references, visited) + evalVisited(b, references, visited)
        case Minus(a,b) => evalVisited(a, references, visited) - evalVisited(b, references, visited)
        case Times(a,b) => evalVisited(a, references, visited) * evalVisited(b, references, visited)
        case Divide(a,b) => evalVisited(a, references, visited) / evalVisited(b, references, visited)
      }
    }
    evalVisited(expr, references, Set())
  }


  /** Get the Expr for a referenced variables.
   *  If the variable is not known, returns a literal NaN.
   */
  private def getReferenceExpr(name: String,
                               references: Map[String, Signal[Expr]]) = {
    references.get(name).fold[Expr] {
      Literal(Double.NaN)
    } { exprSignal =>
      exprSignal()
    }
  }
}
