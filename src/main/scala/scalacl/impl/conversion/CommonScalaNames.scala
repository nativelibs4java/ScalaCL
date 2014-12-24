package scalacl.impl

import scala.language.implicitConversions
import scala.language.postfixOps

import scala.reflect.api.Universe
import scala.reflect.NameTransformer

import scalaxy.streams.Utils

trait CommonScalaNames extends Utils {
  val global: Universe
  import global._
  import definitions._

  lazy val ScalaMathPackage =
    rootMirror.staticModule("scala.math.package")

  lazy val ScalaMathPackageClass =
    ScalaMathPackage.moduleClass

  object ScalaMathFunction {
    /**
     * I'm all for avoiding "magic strings" but in this case it's hard to
     *  see the twice-as-long identifiers as much improvement.
     */
    def apply(functionName: String, args: List[Tree]) =
      q"$ScalaMathPackage.${TermName(functionName)}(..$args)"
    // q"scala.math.`package`.${TermName(functionName)}(..$args)"

    def unapply(tree: Tree): Option[(Type, Name, List[Tree])] = tree match {
      case Apply(f @ Select(left, name), args) =>
        if (left.symbol == ScalaMathPackage ||
          left.symbol == ScalaMathPackageClass ||
          left.tpe == ScalaMathPackageClass.asType.toType)
          Some((f.tpe, name, args))
        else if (tree.symbol != NoSymbol &&
          tree.symbol.owner == ScalaMathPackage) // ScalaMathCommonClass)
          Some((f.tpe, name, args))
        else
          None
      case _ =>
        None
    }
  }

}
