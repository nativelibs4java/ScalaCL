package scalacl

import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalamock.scalatest.MockFactory

import scalaxy.components.{WithTestFresh, WithRuntimeUniverse}

trait BaseTest extends FlatSpecLike with Matchers with MockFactory{
  def context[T](f: Context => T): T = {
    val context = Context.best
    try {
      f(context)
    } finally context.release()
  }
}


trait RuntimeUniverseTest extends WithRuntimeUniverse with WithTestFresh
