/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

import scala.reflect.api.Universe

trait CodeGeneration extends CodeConversion {
  val global: Universe
  import global._
  import definitions._

  private[impl] def expr[T](tree: Tree): Expr[T] = {
    import scala.reflect.api.Mirror
    import scala.reflect.api.TreeCreator
    Expr[T](rootMirror, new TreeCreator {
      def apply[U <: Universe with Singleton](m: Mirror[U]) = {
        tree.asInstanceOf[U#Tree]
      }
    })
  }

  private[impl] def ident[T](vd: ValDef) = expr[T](Ident(vd.name))

  def blockToUnitFunction(block: Tree) = {
    expr[Unit => Unit](
      Function(
        List(
          ValDef(NoMods, newTermName(fresh("noarg")), TypeTree(UnitTpe), EmptyTree)
        ),
        block
      )
    )
  }

  def freshVal(nameBase: String, tpe: Type, rhs: Tree): ValDef = {
    val name = newTermName(fresh(nameBase))
    ValDef(Modifiers(), name, TypeTree(tpe), rhs)
  }

  def convertFunction[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B], kernelId: Long,
    inputTpe: Type,
    outputSymbol: Symbol,
    outputTpe: Type): Expr[CLFunction[A, B]] = {

    def isUnit(t: Type) =
      t <:< UnitTpe || t == NoType

    val Function(params, body) = typeCheck(f.tree, WildcardType)

    val bodyToConvert =
      if (isUnit(outputTpe)) {
        body
      } else {
        Assign(setType(Ident(outputSymbol), outputTpe), body)
      }

    val inputParamDesc: Option[ParamDesc] = if (isUnit(inputTpe.asInstanceOf[global.Type])) None else Some({
      val List(param) = params
      ParamDesc(
        symbol = castSymbol(param.symbol),
        tpe = castType(inputTpe),
        mode = ParamKind.ImplicitArrayElement,
        usage = UsageKind.Input,
        implicitIndexDimension = Some(0))
    })

    val outputParamDesc: Option[ParamDesc] = if (isUnit(outputTpe.asInstanceOf[global.Type])) None else Some({
      ParamDesc(
        symbol = castSymbol(outputSymbol),
        tpe = castType(outputTpe),
        mode = ParamKind.ImplicitArrayElement,
        usage = UsageKind.Output,
        implicitIndexDimension = Some(0))
    })

    val result = generateCLFunction[A, B](
      f = castExpr(f),
      kernelId = kernelId,
      body = castTree(bodyToConvert),
      paramDescs = inputParamDesc.toSeq ++ outputParamDesc.toSeq
    )
    result
  }

  private[impl] def generateCLFunction[A: WeakTypeTag, B: WeakTypeTag](
    f: Expr[A => B],
    kernelId: Long,
    body: Tree,
    paramDescs: Seq[ParamDesc]): Expr[CLFunction[A, B]] =
    {
      println("Generating CL function for " + f)
      try {
        val cr @ CodeConversionResult(code, capturedInputs, capturedOutputs, capturedConstants) = convertCode(
          body,
          paramDescs
        )

        val codeExpr = expr[String](Literal(Constant(code)))
        val kernelIdExpr = expr[Long](Literal(Constant(kernelId)))

        def ident(s: global.Symbol) =
          Ident(s.asInstanceOf[Symbol].name)
        // Ident(s.asInstanceOf[Symbol])

        val inputs = arrayApply[CLArray[_]](
          capturedInputs
            .map(d => ident(d.symbol)).toList
        )
        val outputs = arrayApply[CLArray[_]](
          capturedOutputs
            .map(d => ident(d.symbol)).toList
        )
        val constants = arrayApply[AnyRef](
          capturedConstants
            .map(d => {
              val x = expr[Array[AnyRef]](ident(d.symbol))
              (reify { x.splice.asInstanceOf[AnyRef] }).tree
            }).toList
        )
        //println(s"""
        //  code: $code
        //  capturedInputs: $capturedInputs, 
        //  capturedOutputs: $capturedOutputs, 
        //  capturedConstants: $capturedConstants""") 
        reify {
          new CLFunction[A, B](
            f.splice,
            new Kernel(codeExpr.splice, id = Some(kernelIdExpr.splice)),
            Captures(
              inputs = inputs.splice,
              outputs = outputs.splice,
              constants = constants.splice)
          )
        }
      } catch {
        case ex: Throwable =>
          // ex.printStackTrace()
          sys.error("CLFunction generation failed for { " + f + " }: " + ex)
          null
      }
    }

  private def arrayApply[A: TypeTag](values: List[Tree]): Expr[Array[A]] = {
    import definitions._
    expr[Array[A]](
      Apply(
        TypeApply(
          Select(Ident(ArrayModule), newTermName("apply")),
          List(TypeTree(typeOf[A]))
        ),
        values
      )
    )
  }

}
