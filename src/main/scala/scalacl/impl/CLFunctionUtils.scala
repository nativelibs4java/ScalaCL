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
import scala.reflect.runtime.universe.WeakTypeTag

import scalaxy.components.WithRuntimeUniverse
import scalaxy.reified.internal.Utils
import scalaxy.reified.internal.Utils.reflectMethod
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.Utils.optimisingToolbox
import scalaxy.reified.internal.Optimizer.{ optimize, getFreshNameGenerator }

object CLFunctionUtils {
  def functionKernel[A: WeakTypeTag, B: WeakTypeTag](f: CLFunction[A, B]): FunctionKernel /*[A, B]*/ = {
    val toolbox = optimisingToolbox

    val (expr, captures) = f.value.expr()
    val ast = expr.tree

    type Result = ru.Tree
    val generation = new CodeGeneration with WithRuntimeUniverse with WithResult[Result] {

      import global._

      // println(s"""
      //   ast: $ast
      //   captures: $captures
      // """)
      val ff = ru.Function(captures.map({
        case (name, cap) =>
          ru.ValDef(ru.NoMods, ru.newTermName(name), ru.TypeTree(cap.tpe.asInstanceOf[ru.Type]), ru.EmptyTree)
      }).toList, ast.asInstanceOf[ru.Tree])

      val optimizedAST = optimize(ff, toolbox)
      // val optimizedAST = toolbox.resetLocalAttrs(ast)
      // val body = optimizedAST match {
      //   case ru.Function(_, ru.Block(Nil, ru.Function(List(_), body))) => body
      //   case ru.Function(_, ru.Function(List(_), body)) => body
      // }

      val freshName = getFreshNameGenerator(ast)
      def fresh(s: String) = freshName(s).toString

      val outputSymbol = NoSymbol.newTermSymbol(newTermName(fresh("out")))

      val result = functionToFunctionKernel /*[A, B]*/ (
        captureFunction = castTree(optimizedAST),
        kernelSalt = -1,
        outputSymbol = castSymbol(outputSymbol)).asInstanceOf[Result]
    }

    // println(s"FUNCTION EXPR: $functionExpr")
    val compiled = CompilerUtils.compile(generation.result, toolbox)
    val method = reflectMethod(compiled(), "apply")
    val args = captures.map(_._2.value).toArray
    // println(s"""
    //   METHOD: $method
    //   ARGS: $args
    // """)
    method(args: _*).asInstanceOf[FunctionKernel /*[A, B]*/ ]
  }
}
