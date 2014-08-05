package scalacl

import org.scalatest.{ FlatSpecLike, Matchers }

trait BaseTest extends FlatSpecLike with Matchers {
  def context[T](f: Context => T): T = {
    val context = Context.best
    try {
      f(context)
    } finally context.release()
  }
}

