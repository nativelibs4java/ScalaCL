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

package object scalacl extends impl.TupleDataIOImplicits {
  import impl._

  // implicit lazy val byteDataIO = byteDataIO_
  // private def byteDataIO_(implicit m: Manifest[Byte]) = macro ScalarDataIOMacros.scalarDataIO[Byte]

  // implicit lazy val shortDataIO = shortDataIO_
  // private def shortDataIO_(implicit m: Manifest[Short]) = macro ScalarDataIOMacros.scalarDataIO[Short]

  // implicit lazy val intDataIO = intDataIO_
  // private def intDataIO_(implicit m: Manifest[Int]) = macro ScalarDataIOMacros.scalarDataIO[Int]

  // implicit lazy val longDataIO = longDataIO_
  // private def longDataIO_(implicit m: Manifest[Long]) = macro ScalarDataIOMacros.scalarDataIO[Long]

  // implicit lazy val floatDataIO = floatDataIO_
  // private def floatDataIO_(implicit m: Manifest[Float]) = macro ScalarDataIOMacros.scalarDataIO[Float]

  // implicit lazy val doubleDataIO = doubleDataIO_
  // private def doubleDataIO_(implicit m: Manifest[Double]) = macro ScalarDataIOMacros.scalarDataIO[Double]

  // implicit lazy val booleanDataIO = booleanDataIO_
  // private def booleanDataIO_(implicit m: Manifest[Boolean]) = macro ScalarDataIOMacros.scalarDataIO[Boolean]

  implicit val byteDataIO = ByteDataIO
  implicit val shortDataIO = ShortDataIO
  implicit val intDataIO = IntDataIO
  implicit val longDataIO = LongDataIO
  implicit val floatDataIO = FloatDataIO
  implicit val doubleDataIO = DoubleDataIO
  implicit val booleanDataIO = BooleanDataIO

  implicit class ArrayConversions[A: Manifest: DataIO](array: Array[A])(implicit context: Context) {
    def toCLArray = CLArray[A](array: _*)
    def cl = toCLArray
  }

  def kernel(block: Unit)(implicit contextExpr: Context): Unit = macro KernelMacros.kernelImpl

  def task(block: Unit)(implicit contextExpr: Context): Unit = macro KernelMacros.taskImpl
}
