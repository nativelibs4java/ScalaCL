
//  * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
//  * http://scalacl.googlecode.com/
//  *
//  * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
//  * All rights reserved.
//  *
//  * Redistribution and use in source and binary forms, with or without
//  * modification, are permitted provided that the following conditions are met:
//  *
//  *     * Redistributions of source code must retain the above copyright
//  *       notice, this list of conditions and the following disclaimer.
//  *     * Redistributions in binary form must reproduce the above copyright
//  *       notice, this list of conditions and the following disclaimer in the
//  *       documentation and/or other materials provided with the distribution.
//  *     * Neither the name of Olivier Chafik nor the
//  *       names of its contributors may be used to endorse or promote products
//  *       derived from this software without specific prior written permission.
//  *
//  * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
//  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//  * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
//  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
//  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
//  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
//  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// package scalacl
// package impl

// object ScalarDataIOs {
//   def scalarDataIO[A: c.WeakTypeTag](c: Context)(m: c.Expr[Manifest[A]]): c.Expr[ScalarDataIO[A]] = {
//     import c.universe._
//     val ta = weakTypeTag[A].tpe
//     val name = ta.typeSymbol.name.toString

//     def term(n: String) = Ident(TermName(n))
//     def sel(target: Tree, n: String) = Select(target, TermName(n))

//     val pointerIOExpr = c.Expr[PointerIO[A]](
//       sel(
//         Ident(typeOf[PointerIO[_]].typeSymbol.asClass.companionSymbol),
//         s"get${name}Instance"))
//     def pointerTree =
//       Apply(
//         term("buffers"),
//         List(
//           term("bufferOffset")))
//     val getBodyExpr = c.Expr[A](
//       Apply(
//         sel(pointerTree, s"get${name}AtIndex"),
//         List(term("index"))))
//     val setBodyExpr = c.Expr[A](
//       Apply(
//         sel(pointerTree, s"set${name}AtIndex"),
//         List(
//           term("index"),
//           term("value"))))
//     val res = reify {
//       implicit val mm = m.splice
//       new ScalarDataIO[A](pointerIOExpr.splice) {
//         override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
//           getBodyExpr.splice
//         override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: A) =
//           setBodyExpr.splice
//       }
//     }

//     println(res)

//     res
//   }
// }

// trait CommonScalaDataIOs {

//   implicit lazy val byteDataIO: DataIO[Byte] = byteDataIO_
//   private def byteDataIO_(implicit m: Manifest[Byte]): DataIO[Byte] = macro ScalarDataIOMacros.scalarDataIO[Byte]

//   implicit lazy val shortDataIO: DataIO[Short] = shortDataIO_
//   private def shortDataIO_(implicit m: Manifest[Short]): DataIO[Short] = macro ScalarDataIOMacros.scalarDataIO[Short]

//   implicit lazy val intDataIO: DataIO[Int] = intDataIO_
//   private def intDataIO_(implicit m: Manifest[Int]): DataIO[Int] = macro ScalarDataIOMacros.scalarDataIO[Int]

//   implicit lazy val longDataIO: DataIO[Long] = longDataIO_
//   private def longDataIO_(implicit m: Manifest[Long]): DataIO[Long] = macro ScalarDataIOMacros.scalarDataIO[Long]

//   implicit lazy val booleanDataIO: DataIO[Boolean] = booleanDataIO_
//   private def booleanDataIO_(implicit m: Manifest[Boolean]): DataIO[Boolean] = macro ScalarDataIOMacros.scalarDataIO[Boolean]

//   implicit lazy val floatDataIO: DataIO[Float] = floatDataIO_
//   private def floatDataIO_(implicit m: Manifest[Float]): DataIO[Float] = macro ScalarDataIOMacros.scalarDataIO[Float]

//   implicit lazy val doubleDataIO: DataIO[Double] = doubleDataIO_
//   private def doubleDataIO_(implicit m: Manifest[Double]): DataIO[Double] = macro ScalarDataIOMacros.scalarDataIO[Double]
// }
