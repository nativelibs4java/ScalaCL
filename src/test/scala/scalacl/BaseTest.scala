package scalacl

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpecLike, Matchers }

import scala.reflect.runtime.{ currentMirror => cm, universe => ru }
import scala.tools.reflect.ToolBox
import scalacl.impl.{ Vectorization, CodeConversion, OpenCLCodeFlattening }
import scalaxy.components.FlatCode

trait BaseTest extends FlatSpecLike with Matchers with MockFactory {
  def context[T](f: Context => T): T = {
    val context = Context.best
    try {
      f(context)
    } finally context.release()
  }
}

trait RuntimeUniverseTest {
  lazy val global = ru
  import global._

  def verbose = false

  private var nextId = 0L

  def fresh(s: String) = synchronized {
    val v = nextId
    nextId += 1
    s + v
  }

  def warning(pos: Position, msg: String) =
    println(msg + " (" + pos + ")")

  def withSymbol[T <: Tree](sym: Symbol, tpe: Type = NoType)(tree: T): T = tree

  def typed[T <: Tree](tree: T): T = {
    // if (tree.tpe == null && tree.tpe == NoType)
    //   toolbox.typeCheck(tree.asInstanceOf[toolbox.u.Tree]).asInstanceOf[T]
    // else
    tree
  }

  def inferImplicitValue(pt: Type): Tree =
    toolbox.inferImplicitValue(pt.asInstanceOf[toolbox.u.Type]).asInstanceOf[global.Tree]

  lazy val toolbox = cm.mkToolBox()

  def typeCheck(x: Expr[_]): Tree =
    typeCheck(x.tree)

  def typeCheck(tree: Tree, pt: Type = WildcardType): Tree = {
    val ttree = tree.asInstanceOf[toolbox.u.Tree]
    if (ttree.tpe != null && ttree.tpe != NoType)
      tree
    else {
      try {
        toolbox.typecheck(
          ttree,
          pt = pt.asInstanceOf[toolbox.u.Type])
      } catch {
        case ex: Throwable =>
          throw new RuntimeException(s"Failed to typeCheck($tree, $pt): $ex", ex)
      }
    }.asInstanceOf[Tree]
  }

  def resetLocalAttrs(tree: Tree): Tree = {
    toolbox.untypecheck(tree.asInstanceOf[toolbox.u.Tree]).asInstanceOf[Tree]
  }

}

trait CodeConversionTest extends CodeConversion with RuntimeUniverseTest {
  val global: reflect.api.Universe

  import global._

  def convertExpression(block: Expr[Unit], explicitParamDescs: Seq[ParamDesc] = Seq()) = {
    convertCode(typeCheck(block.tree, WildcardType), explicitParamDescs)
  }

  def flatStatement(statements: Seq[String], values: Seq[String]): FlatCode[String] =
    FlatCode[String](statements = statements, values = values)

  def flatAndConvertExpression(x: Expr[_]): FlatCode[String] = {
    flattenAndConvert(typeCheck(x))
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

trait CodeVectorizationTest extends Vectorization with RuntimeUniverseTest
