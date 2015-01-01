package scalacl

import com.nativelibs4java.opencl.JavaCL
import com.nativelibs4java.opencl.CLPlatform

import scalacl.impl.KernelDef

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

package ops {

  object KernelStrings {
    lazy val context = JavaCL.createBestContext(CLPlatform.DeviceFeature.CPU)

    def clImpl(c: blackbox.Context)(args: c.Expr[Any]*): c.Expr[KernelDef] = {
      object Utils extends KernelStringUtils {
        override val u = c.universe
        override def fresh(name: String) = c.freshName(name)

        import u._

        val q"${ _ }.${ _ }($stringContext)" = c.prefix.tree.asInstanceOf[Tree]
        val q"scala.StringContext.apply(..$parts)" = stringContext

        var canCompile = true
        val (argStrings, formattedArgExprs) = args.map(_.tree.asInstanceOf[Tree]).map(arg => {
          val formatted = Option(formatConstantTree(arg, arg.tpe)).getOrElse {
            canCompile = false
            c.error(arg.pos.asInstanceOf[c.universe.Position],
              s"Constants of type ${arg.tpe} are not supported.")
            q"null"
          }
          val constant = Option(formatDefaultConstant(arg.tpe)).getOrElse {
            "?"
          }
          (s"$constant /* ${arg.toString.replace("*/", "*//")} */", formatted)
        }).toList.unzip

        val result = if (canCompile) {
          val partStrings = parts.map({
            case Literal(Constant(part: String)) =>
              part
          })
          val source = partStrings.
            zip("" :: argStrings).
            map({ case (part, arg) => arg + part }).
            mkString("")

          println(s"SOURCE: $source")
          val program = context.createProgram(source)
          program.addBuildOption("-Werror")
          // TODO: clCompileProgram instead
          // TODO: parse errors / warnings
          program.build()

          val expr = parts.zip((null: Tree) :: formattedArgExprs).
            map({
              case (part, arg) =>
                if (arg == null)
                  part
                else
                  q"$arg + $part"
            }).
            foldLeft(q""" "" """: Tree)({
              case (a, b) =>
                q"$a + $b"
            })

          q"""
            new scalacl.impl.KernelDef(
              sources = $expr,
              salt = ${c.macroApplication.pos.start}
            )
          """
        } else {
          q"null"
        }
      }
      c.Expr[KernelDef](Utils.result.asInstanceOf[c.universe.Tree])
    }
  }
}

package object ops {
  implicit class KernelStringContext(val sc: StringContext) extends AnyVal {
    def cl(args: Any*): KernelDef = macro KernelStrings.clImpl
  }
}
