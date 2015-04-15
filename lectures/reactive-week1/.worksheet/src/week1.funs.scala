package week1

object funs {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(72); 
  println("Welcome to the Scala worksheet");$skip(48); 

  val f: (Int => String) = List("a", "b", "c");System.out.println("""f  : Int => String = """ + $show(f ));$skip(7); val res$0 = 
  f(2);System.out.println("""res0: String = """ + $show(res$0));$skip(74); 
  
  val fun: PartialFunction[String, String] = { case "ping" => "pong" };System.out.println("""fun  : PartialFunction[String,String] = """ + $show(fun ));$skip(14); val res$1 = 
  fun("ping");System.out.println("""res1: String = """ + $show(res$1));$skip(26); val res$2 = 
  fun.isDefinedAt("ping");System.out.println("""res2: Boolean = """ + $show(res$2))}

}
