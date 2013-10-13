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
// package scalacl
// package impl

// import scala.reflect.ClassTag
// import scala.collection.mutable.ArrayBuffer
// import scala.reflect.macros.Context

// import com.nativelibs4java.opencl.CLMem
// import org.bridj.{ Pointer, PointerIO }

// object ScalarDataIOMacros {
//   def scalarDataIO[A: c.WeakTypeTag](c: Context)(m: c.Expr[Manifest[A]]): c.Expr[ScalarDataIO[A]] = {
//     import c.universe._
//     val ta = weakTypeTag[A].tpe
//     val name = ta.typeSymbol.name.toString

//     def term(n: TermName) = Ident(n)
//     def sel(target: Tree, n: TermName) = Select(target, n)

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
//     reify {
//       implicit val mm = m.splice
//       new ScalarDataIO[A](pointerIOExpr.splice) {
//         override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
//           getBodyExpr.splice
//         override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Int) =
//           setBodyExpr.splice
//       }
//     }
//   }
// }
