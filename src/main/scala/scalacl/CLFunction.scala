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

import scalacl.impl.CLFunctionLike
import scalacl.impl.CLFunctionMacros
import scalacl.impl.CLFunctionUtils
import scalacl.impl.FunctionKernel

import scala.language.experimental.macros
import scala.language.implicitConversions

import scalaxy.reified._
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.WeakTypeTag

case class CLFunction[A: WeakTypeTag, B: WeakTypeTag](value: Reified[A => B], preparedFunctionKernel: Option[FunctionKernel])
    extends (A => B)
    with CLFunctionLike[A, B] {

  lazy val function = value.value
  lazy val functionKernel =
    preparedFunctionKernel.getOrElse(
      CLFunctionUtils.functionKernel[A, B](this))
}

object CLFunction {
  implicit def fun2clfun[A: TypeTag, B: TypeTag](f: A => B): CLFunction[A, B] = macro CLFunctionMacros.fun2clfun[A, B]
}
