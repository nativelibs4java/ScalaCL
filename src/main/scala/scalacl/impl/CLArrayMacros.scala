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
import scalacl.CLArray
import scalacl.CLFilteredArray

import language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.ClassTag
import scala.reflect.runtime.universe

private[scalacl] object CLArrayMacros {
  def typeTagExpr[T: c.WeakTypeTag](c: Context) = {
    import c.universe._
    c.Expr[universe.TypeTag[T]](
      c.inferImplicitValue(
        weakTypeTag[universe.TypeTag[T]].tpe))
  }

  def foreachImpl[T: c.WeakTypeTag](c: Context)(f: c.Expr[T => Unit]): c.Expr[Unit] = {
    val ff = CLFunctionMacros.fun2clfun[T, Unit](c)(f)(typeTagExpr[T](c), typeTagExpr[Unit](c))
    c.universe.reify({
      val self = c.prefix.asInstanceOf[c.Expr[CLArray[T]]].splice
      self.foreach(ff.splice)
    })
  }
  def mapImpl[T: c.WeakTypeTag, U: c.WeakTypeTag](c: Context)(f: c.Expr[T => U])(io2: c.Expr[DataIO[U]], m2: c.Expr[ClassTag[U]], t2: c.Expr[universe.TypeTag[U]]): c.Expr[CLArray[U]] = {
    // try {
    val ff = CLFunctionMacros.fun2clfun[T, U](c)(f)(typeTagExpr[T](c), typeTagExpr[U](c))
    // c.Expr[CLArray[U]](
    // c.typeCheck(
    c.universe.reify({
      val self = c.prefix.asInstanceOf[c.Expr[CLArray[T]]].splice
      import self.t
      self.map[U](ff.splice)(io2.splice, m2.splice, t2.splice)
    }) //.tree
    //     )
    //   )
    // } catch {
    //   case ex: Throwable =>
    //     ex.printStackTrace()
    //     println(s"""
    //       MAPPING:
    //         f: $f
    //         io2: $io2
    //         m2: $m2
    //         t2: $t2
    //     """)
    //     throw ex
    // }
  }
  def filterImpl[T: c.WeakTypeTag](c: Context)(f: c.Expr[T => Boolean]): c.Expr[CLFilteredArray[T]] = {
    val ff = CLFunctionMacros.fun2clfun[T, Boolean](c)(f)(typeTagExpr[T](c), typeTagExpr[Boolean](c))
    c.universe.reify {
      val self = c.prefix.asInstanceOf[c.Expr[CLArray[T]]].splice
      import self.t
      self.filter(ff.splice)
    }
  }
}
