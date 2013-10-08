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

import scala.reflect.runtime.{ universe => ru }
import scala.reflect.runtime.universe.TypeTag

import scalaxy.components.WithRuntimeUniverse
import scalaxy.reified.internal.Utils
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.Utils.optimisingToolbox
import scalaxy.reified.internal.Optimizer.{ optimize, getFreshNameGenerator }

object CLReifiedFunctionUtils {
  def convert[A: TypeTag, B: TypeTag](f: CLReifiedFunction[A, B]): CLFunction[A, B] = {
    val toolbox = optimisingToolbox

    val generation = new CodeGeneration with WithRuntimeUniverse with WithResult[ru.Expr[CLFunction[A, B]]] {

      import global._
      val (expr, captures) = f.value.expr()
      val ast = expr.tree

      val optimizedAST = optimize(ast, toolbox)
      val body = optimizedAST match {
        case ru.Block(Nil, ru.Function(List(_), body)) => body
        case ru.Function(List(_), body) => body
        // case ru.Block(ru.Function(List(_), body), EmptyTree) => body
      }
      // val ru.Function(List(_), body) = optimizedAST

      // println(s"""
      //   f.value: ${f.value}
      //   ast: $ast
      //   optimizedAST: $optimizedAST
      //   param: $param
      //   param.symbol: ${param.symbol}
      //   captures: $captures
      // """)

      val freshName = getFreshNameGenerator(ast)
      def fresh(s: String) = freshName(s).toString

      val outputSymbol = NoSymbol.newTermSymbol(newTermName(fresh("out")))

      val result = convertFunction[A, B](
        f = expr[A => B](castTree(optimizedAST)),
        kernelSalt = -1,
        outputSymbol = castSymbol(outputSymbol)).asInstanceOf[ru.Expr[CLFunction[A, B]]]
    }
    val functionExpr = generation.result
    // println(s"FUNCTION EXPR: $functionExpr")
    val compiled = CompilerUtils.compile(functionExpr.tree, toolbox)
    compiled().asInstanceOf[CLFunction[A, B]]
  }
}
