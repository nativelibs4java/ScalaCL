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
import scala.reflect.runtime.universe.{ internal => ri }
import scala.reflect.runtime.universe.WeakTypeTag

import scalaxy.components.WithRuntimeUniverse
import scalaxy.reified.internal.Utils.getFreshNameGenerator
import scalaxy.reified.internal.Utils.getMethodMirror
import scalaxy.reified.internal.CompilerUtils
import scalaxy.reified.internal.Utils.optimisingToolbox

import scalaxy.generic.trees.simplifyGenericTree

object CLFunctionUtils {
  def functionKernel[A: WeakTypeTag, B: WeakTypeTag](f: CLFunction[A, B]): FunctionKernel = {
    val toolbox = optimisingToolbox

    var ast = f.value.expr.tree
    val captures = ast collect {
      case t if t.symbol != null && ri.isFreeTerm(t.symbol) =>
        ri.asFreeTerm(t.symbol)
    }
    ast = simplifyGenericTree(toolbox.typeCheck(ast, f.value.valueTag.tpe))
    // println("SIMPLIFIED AST: " + ast)
    ast = toolbox.resetLocalAttrs(ast)
    //ast = toolbox.typeCheck(ast, f.value.valueTag.tpe)

    type Result = ru.Tree
    val generation = new CodeGeneration with WithRuntimeUniverse with WithResult[Result] {

      import global._

      // println(s"""
      //   ast: $ast
      //   captures: $captures
      // """)
      val ff = ru.Function(captures.map({
        case fsym =>
          ru.ValDef(ru.NoMods, ru.newTermName(fsym.name.toString), ru.TypeTree(fsym.typeSignature), ru.EmptyTree)
      }).toList, ast)

      val freshName = getFreshNameGenerator(ast)
      def fresh(s: String) = freshName(s).toString

      val outputSymbol = internal.newTermSymbol(NoSymbol, fresh("out"))

      val result = functionToFunctionKernel(
        captureFunction = castTree(ff),
        kernelSalt = -1,
        outputSymbol = castSymbol(outputSymbol)).asInstanceOf[Result]
    }

    val compiled = CompilerUtils.compile(generation.result, toolbox)
    val method = getMethodMirror(compiled(), "apply")
    val args = captures.map(_.value).toArray
    // println(s"""
    //   METHOD: $method
    //   ARGS: $args
    // """)
    method(args: _*).asInstanceOf[FunctionKernel]
  }
}
