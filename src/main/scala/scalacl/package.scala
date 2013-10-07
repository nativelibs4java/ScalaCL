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
import com.nativelibs4java.opencl.CLMem

import scala.language.experimental.macros
import scala.language.implicitConversions

import scalaxy.reified._
import scala.reflect.runtime.universe.TypeTag

package object scalacl {
  import impl._

  implicit val intDataIO = IntDataIO
  implicit val floatDataIO = FloatDataIO
  implicit val booleanDataIO = BooleanDataIO
  implicit def tuple2DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO] = {
    new Tuple2DataIO[T1, T2]
  }

  implicit class ArrayConversions[A: Manifest: DataIO](array: Array[A])(implicit context: Context) {
    def toCLArray = CLArray[A](array: _*)
    def cl = toCLArray
  }

  def kernel(block: Unit)(implicit contextExpr: Context): Unit = macro KernelMacros.kernelImpl

  def task(block: Unit)(implicit contextExpr: Context): Unit = macro KernelMacros.taskImpl
}

package scalacl {
  class Precomp(sources: String*)
  class CLFunc[A, B](val value: ReifiedValue[A => B], val precomp: Precomp) extends (A => B) {
    def apply(a: A): B = value.value(a)
  }
  object CLFunc {
    implicit def fun2clfun[A: TypeTag, B: TypeTag](f: A => B): CLFunc[A, B] = macro internal.fun2clfun[A, B]

    object internal {
      def fun2clfun[A: c.WeakTypeTag, B: c.WeakTypeTag](c: scala.reflect.macros.Context)(f: c.Expr[(A => B)])(ta: c.Expr[TypeTag[A]], tb: c.Expr[TypeTag[B]]): c.Expr[CLFunc[A, B]] = {
        //import c.universe._

        // TODO: use Reified API to create reified value here, choking appropriately upon unsupported captures.

        // TODO: static compilation here.
        c.universe.reify {
          implicit val tta = ta.splice
          implicit val ttb = tb.splice
          new CLFunc[A, B](f.splice, new Precomp(""))
        }
      }
    }
  }
}
