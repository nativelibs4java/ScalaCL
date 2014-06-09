package scalacl

import scala.reflect.macros.blackbox

package object impl {
  def typeCheckOrTrace[A](c: blackbox.Context)(msg: => String)(block: => c.Expr[A]): c.Expr[A] = {
    import c.universe._
    tryOrTrace(msg) {
      c.Expr[A](c.typeCheck(block.tree))
    }
  }
  def tryOrTrace[A](msg: => String)(block: => A): A = {
    try {
      block
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        println("ERROR: " + msg)
        throw ex
    }
  }
}
