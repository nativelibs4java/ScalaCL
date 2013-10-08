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

import scalaxy.reified._
import scala.reflect.runtime.universe.TypeTag

object CLReifiedFunctionMacros {
  def fun2clfun[A: c.WeakTypeTag, B: c.WeakTypeTag](c: scala.reflect.macros.Context)(f: c.Expr[(A => B)])(ta: c.Expr[TypeTag[A]], tb: c.Expr[TypeTag[B]]): c.Expr[CLReifiedFunction[A, B]] = {

    // TODO: choking appropriately upon unsupported captures.

    // Attempt to pre-convert the function.
    // This may fail if the tree contains free types: in that case, the reified value
    // tree will need to be converted at runtime.
    val precompiledFunctionExpr: c.Expr[Option[CLFunction[A, B]]] =
      // try {
      //   val expr = CLFunctionMacros.convertFunction[A, B](c)(f)
      //   c.universe.reify(Some(expr.splice))
      // } catch {
      //   case ex: Throwable =>
      //     ex.printStackTrace()
      c.universe.reify(None)
    // }

    // TODO: perform static precompilation here.
    c.universe.reify {
      implicit val tta = ta.splice
      implicit val ttb = tb.splice
      new CLReifiedFunction[A, B](
        f.splice,
        precompiledFunctionExpr.splice)
    }
  }
}
