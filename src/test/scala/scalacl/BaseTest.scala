package scalacl

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpecLike, Matchers }
import scalacl.impl.{ OpenCLCodeFlattening, CodeConversion }
import scalaxy.components.{ FlatCode, WithRuntimeUniverse }

trait BaseTest extends FlatSpecLike with Matchers with MockFactory {
  def context[T](f: Context => T): T = {
    val context = Context.best
    try {
      f(context)
    } finally context.release()
  }
}

trait RuntimeUniverseTest extends WithRuntimeUniverse {
  private var nextId = 0L

  def fresh(s: String) = synchronized {
    val v = nextId
    nextId += 1
    s + v
  }
}

trait CodeConversionTest extends CodeConversion with RuntimeUniverseTest {
  val global: reflect.api.Universe
  import global._

  def convertExpression(block: Expr[Unit], explicitParamDescs: Seq[ParamDesc] = Seq()) = {
    convertCode(typeCheck(block.tree, WildcardType), explicitParamDescs)
  }

  def flatAndConvertExpression(x: Expr[_]): FlatCode[String] = {
    flattenAndConvert(typeCheck(x))
  }

  def flatCode(statements: Seq[String], values: Seq[String]): FlatCode[String] =
    FlatCode[String](statements = statements, values = values)
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
      statements = statements.map(x => unwrap(typeCheck(x.tree, WildcardType))),
      values = values.map(x => unwrap(typeCheck(x.tree, WildcardType)))
    )
  }

  def inputSymbols(xs: Expr[_]*): Seq[(Symbol, Type)] = {
    for (x <- xs.toSeq) yield {
      val i @ Ident(n) = typeCheck(x.tree, WildcardType)
      (i.symbol, i.tpe)
    }
  }

  def flatExpression(x: Expr[_], inputSymbols: Seq[(Symbol, Type)] = Seq(), owner: Symbol = NoSymbol): FlatCode[Tree] = {
    flatten(typeCheck(x.tree, WildcardType), inputSymbols, owner)
  }
}
