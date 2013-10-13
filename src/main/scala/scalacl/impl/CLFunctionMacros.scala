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

import scalaxy.components.WithMacroContext

import language.experimental.macros
import scala.reflect.macros.Context

private[impl] object CLFunctionMacros {

  private[impl] def convertFunction[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(f: c.Expr[A => B]): c.Expr[FunctionKernel /*[A, B]*/ ] = {
    import c.universe._
    import definitions._

    val outputSymbol = Option(c.enclosingMethod).map(_.symbol).getOrElse(NoSymbol).newTermSymbol(newTermName(c.fresh("out")))

    val func = WithResult(
      new CodeGeneration with WithMacroContext with WithResult[c.Expr[FunctionKernel /*[A, B]*/ ]] {
        override val context = c
        import global._

        val result = functionToFunctionKernel[A, B](
          f = castExpr(f),
          kernelSalt = KernelDef.nextKernelSalt,
          outputSymbol = castSymbol(outputSymbol)).asInstanceOf[Result]
      }
    )
    try {
      c.Expr[FunctionKernel](c.typeCheck(func.tree))
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        println("ERROR WITH: " + func)
        throw ex
    }
  }

  private[impl] def convertTask(c: Context)(block: c.Expr[Unit]): c.Expr[CLFunction[Unit, Unit]] = {
    import c.universe._
    import definitions._

    val func = WithResult(
      new CodeGeneration with WithMacroContext with WithResult[c.Expr[CLFunction[Unit, Unit]]] {
        override val context = c

        // Create a fake Unit => Unit function.

        //var typedBlock = newStreamTransformer(false) transform cleanTypeCheck(block.tree)
        // val typedBlock = c.typeCheck(block.tree)
        val functionKernelExpr = generateFunctionKernel[Unit, Unit](
          kernelSalt = KernelDef.nextKernelSalt,
          body = castTree(block.tree),
          paramDescs = Seq()
        )

        val f = blockToUnitFunction(castTree(block.tree))
        val result = reify(
          new CLFunction[Unit, Unit](f.splice, functionKernelExpr.splice)
        ).asInstanceOf[Result]
      }
    )
    try {
      c.Expr[CLFunction[Unit, Unit]](c.typeCheck(func.tree))
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        println("ERROR WITH: " + func)
        throw ex
    }
  }
}
