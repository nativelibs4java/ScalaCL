package scalacl

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpecLike, Matchers }

import scala.reflect.runtime.{ currentMirror => cm, universe => ru }
import scala.tools.reflect.ToolBox
import scalacl.impl.{ Vectorization, CodeConversion, OpenCLCodeFlattening, FlatCode }
import scalacl.impl.FlatCodes._

import scalaxy.streams.WithRuntimeUniverse
import scalaxy.streams.testing.WithTestFresh

trait BaseTest extends FlatSpecLike with Matchers with MockFactory {
  def context[T](f: Context => T): T = {
    val context = Context.best
    try {
      f(context)
    } finally context.release()
  }
}

trait RuntimeUniverseTest extends WithRuntimeUniverse with WithTestFresh

trait CodeConversionTest extends CodeConversion with RuntimeUniverseTest {
  val global: reflect.api.Universe

  import global._

  def convertExpression(block: Expr[Unit], explicitParamDescs: Seq[ParamDesc] = Seq()) = {
    convertCode(
      tree = typecheck(block.tree),
      initialParamDescs = explicitParamDescs,
      fresh = fresh,
      typecheck = typecheck(_)
    )
  }

  def flatStatement(statements: Seq[String], values: Seq[String]): FlatCode[String] =
    FlatCode[String](statements = statements, values = values)

  def flatAndConvertExpression(x: Expr[_]): FlatCode[String] = {
    flattenAndConvert(typecheck(x.tree))
  }
}

trait CodeFlatteningTest extends OpenCLCodeFlattening with RuntimeUniverseTest {
  val global: reflect.api.Universe
  import global._

  def unwrap(tree: Tree): Tree = tree match {
    case Block(List(sub), Literal(Constant(()))) => sub
    case _ => tree
  }

  def flatCode(statements: Seq[Expr[_]] = Seq(), values: Seq[Expr[_]] = Seq()) = {
    FlatCode[Tree](
      statements = statements.map(x => unwrap(typecheck(x.tree))),
      values = values.map(x => unwrap(typecheck(x.tree)))
    )
  }

  def inputSymbols(xs: Expr[_]*): Seq[(Symbol, Type)] = {
    for (x <- xs.toSeq) yield {
      val i @ Ident(n) = typecheck(x.tree)
      (i.symbol, i.tpe)
    }
  }

  def flatExpression(x: Expr[_], inputSymbols: Seq[(Symbol, Type)] = Seq(), owner: Symbol = NoSymbol): FlatCode[Tree] = {
    flatten(typecheck(x.tree), inputSymbols, owner)
  }
}

trait CodeVectorizationTest extends Vectorization with RuntimeUniverseTest
