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
package scalacl
package impl

import scala.reflect.api.Universe
import scala.collection.immutable.NumericRange

import scalacl.CLArray
import scalacl.CLFilteredArray

//import scalaxy.components.MiscMatchers
import scalaxy.streams.Streams

trait Vectorization
    extends CodeGeneration // with MiscMatchers
    with Streams {
  val global: Universe
  import global._
  import definitions._

  object PositiveIntConstantOrOne {
    def unapply(treeOpt: Option[Tree]): Option[Int] = Option(treeOpt) collect {
      case Some(Literal(Constant(n: Int))) if n > 0 => n
      case Some(Literal(Constant(n: Long))) if n > 0 => n.toInt
      case None => 1
    }
  }

  private def rangeParamDesc(numTpe: Type, param: ValDef, fromVal: ValDef, byVal: ValDef, dimension: Int): ParamDesc = {
    ParamDesc(
      symbol = param.symbol,
      tpe = numTpe,
      output = false,
      mode = ParamKind.RangeIndex,
      usage = UsageKind.Input,
      implicitIndexDimension = Some(dimension),
      rangeOffset = Some(newTermSymbol(fromVal.name)),
      rangeStep = Some(newTermSymbol(byVal.name)))
  }

  def rangeSize(fromVal: ValDef, toVal: ValDef, byVal: ValDef, isInclusive: Boolean): Expr[Long] = {
    reify {
      val gap = ident[Long](toVal).splice - ident[Long](fromVal).splice
      gap / ident[Long](byVal).splice +
        (
          if (lit(isInclusive).splice ||
            (gap % ident[Long](byVal).splice != 0))
            1
          else
            0
        )
    }
  }
  private[impl] def vectorize(
    context: Expr[scalacl.Context],
    block: Tree,
    fresh: String => String,
    typecheck: Tree => Tree): Option[Expr[Unit]] =
    {
      Option(block) collect {
        case SomeStream(
          Stream(_,
            InlineRangeStreamSource(start, end, by, isInclusive, numTpe),
            List(ForeachOp(Function(List(param), body))), _, _)) =>
          // case Foreach(
          //   NumRange(rangeTpe, numTpe, start, end, PositiveIntConstantOrOne(by), isInclusive, Nil),
          //   Function(List(param), body)
          //   ) =>
          val startVal = freshVal("start", numTpe, start)
          val endVal = freshVal("end", numTpe, end)
          val byVal = freshVal("by", numTpe, Literal(Constant(by)))

          val functionKernelExpr = generateFunctionKernel[Unit, Unit](
            kernelSalt = KernelDef.nextKernelSalt,
            body = body,
            paramDescs = Seq(
              rangeParamDesc(numTpe, param, startVal, byVal, 0)),
            fresh = fresh,
            typecheck = typecheck(_)
          )
          val f = blockToUnitFunction(block)
          val function = reify(new CLFunction[Unit, Unit](f.splice, Some(functionKernelExpr.splice)))

          expr[Unit](
            Block(
              startVal ::
                endVal ::
                byVal ::
                reify(
                  function.splice(
                    context.splice,
                    new KernelExecutionParameters(rangeSize(startVal, endVal, byVal, isInclusive).splice)
                  )
                ).tree :: Nil,
              q"()"
            // Literal(Constant({}))
            )
          )
        // case Foreach(
        //   NumRange(rangeTpe1, numTpe1, from1, to1, PositiveIntConstantOrOne(by1), isInclusive1, Nil),
        //   Function(List(param1),
        //     Foreach(
        //       NumRange(rangeTpe2, numTpe2, from2, to2, PositiveIntConstantOrOne(by2), isInclusive2, Nil),
        //       Function(List(param2), body)))) =>

        //   val fromVal1 = freshVal("from1", numTpe2, from1)
        //   val toVal1 = freshVal("to1", numTpe1, to1)
        //   val byVal1 = freshVal("by1", numTpe1, Literal(Constant(by1)))

        //   val fromVal2 = freshVal("from2", numTpe2, from2)
        //   val toVal2 = freshVal("to2", numTpe2, to2)
        //   val byVal2 = freshVal("by2", numTpe2, Literal(Constant(by2)))

        //   val functionKernelExpr = generateFunctionKernel[Unit, Unit](
        //     kernelSalt = KernelDef.nextKernelSalt,
        //     body = body,
        //     paramDescs = Seq(
        //       rangeParamDesc(numTpe1, param1, fromVal1, byVal1, 0),
        //       rangeParamDesc(numTpe2, param2, fromVal2, byVal2, 1))
        //   )
        //   val f = blockToUnitFunction(block)
        //   val function = reify(new CLFunction[Unit, Unit](f.splice, Some(functionKernelExpr.splice)))

        //   expr[Unit](
        //     Block(
        //       fromVal1 ::
        //         toVal1 ::
        //         byVal1 ::
        //         fromVal2 ::
        //         toVal2 ::
        //         byVal2 ::
        //         reify(
        //           function.splice(
        //             context.splice,
        //             new KernelExecutionParameters(
        //               rangeSize(fromVal1, toVal1, byVal1, isInclusive1).splice,
        //               rangeSize(fromVal2, toVal2, byVal2, isInclusive2).splice
        //             )
        //           )
        //         ).tree :: Nil,
        //       Literal(Constant({}))
        //     )
        //   )
      }
    }
}
