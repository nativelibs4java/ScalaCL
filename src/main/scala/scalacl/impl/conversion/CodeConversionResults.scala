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
package scalacl.impl
// import scalacl.impl.FlatCode
import scalacl.impl.FlatCodes._

import scalacl.CLArray
import scalacl.CLFilteredArray

import scala.reflect.api.Universe
import scala.util.matching.Regex

trait CodeConversionResults {
  val global: Universe
  // def fresh(s: String): String

  import global._
  import definitions._

  sealed trait ParamKind
  object ParamKind {
    case object ImplicitArrayElement extends ParamKind
    case object RangeIndex extends ParamKind
    case object Normal extends ParamKind
  }

  sealed abstract class UsageKind(val isInput: Boolean, val isOutput: Boolean) {
    def merge(usage: UsageKind): UsageKind
  }
  object UsageKind {
    case object Input extends UsageKind(true, false) {
      override def merge(usage: UsageKind) =
        if (usage == Input) Input
        else InputOutput
    }
    case object Output extends UsageKind(false, true) {
      override def merge(usage: UsageKind) =
        if (usage == Output) Output
        else InputOutput
    }
    case object InputOutput extends UsageKind(true, true) {
      override def merge(usage: UsageKind) = this
    }
  }

  case class ParamDesc(
      symbol: Symbol,
      tpe: Type,
      output: Boolean,
      mode: ParamKind,
      usage: UsageKind,
      implicitIndexDimension: Option[Int] = None,
      rangeOffset: Option[Symbol] = None,
      rangeStep: Option[Symbol] = None) {
    assert((mode == ParamKind.ImplicitArrayElement || mode == ParamKind.RangeIndex) == (implicitIndexDimension != None))
    def isArray =
      mode == ParamKind.ImplicitArrayElement || mode == ParamKind.Normal && tpe <:< typeOf[CLArray[_]]

    def name = symbol.name.asInstanceOf[TermName]
  }

  case class CodeConversionResult(
    code: String,
    capturedInputs: Seq[ParamDesc],
    capturedOutputs: Seq[ParamDesc],
    capturedConstants: Seq[ParamDesc])
}
