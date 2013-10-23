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
import scalaxy.reified._
import scala.reflect.runtime.universe.TypeTag

private[scalacl] trait TupleDataIOImplicits {

  private implicit def io[T: Manifest: DataIO] = implicitly[DataIO[T]]

  // implicit class Exts[T](s: Seq[T]) {
  //   def commas = s.mkString(", ")
  // }
  // for (arity <- 2 to 22) {
  //   println(s"""
  //     implicit def tuple${arity}DataIO[${(1 to arity).map(i => s"T$i: Manifest: DataIO").commas}] = {
  //       new TupleDataIO[(${(1 to arity).map(i => s"T$i").commas})](${(1 to arity).map(i => s"io[T$i]").commas})({
  //         case Array(${(1 to arity).map(i => s"t$i: T$i").commas}) => (${(1 to arity).map(i => s"t$i").commas})
  //       })
  //     }
  //   """)
  // }

  implicit def tuple2DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2)](io[T1], io[T2])({
      case Array(t1: T1, t2: T2) => (t1, t2)
    })
  }

  implicit def tuple3DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3)](io[T1], io[T2], io[T3])({
      case Array(t1: T1, t2: T2, t3: T3) => (t1, t2, t3)
    })
  }

  implicit def tuple4DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4)](io[T1], io[T2], io[T3], io[T4])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4) => (t1, t2, t3, t4)
    })
  }

  implicit def tuple5DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5)](io[T1], io[T2], io[T3], io[T4], io[T5])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5) => (t1, t2, t3, t4, t5)
    })
  }

  implicit def tuple6DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6) => (t1, t2, t3, t4, t5, t6)
    })
  }

  implicit def tuple7DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7) => (t1, t2, t3, t4, t5, t6, t7)
    })
  }

  implicit def tuple8DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8) => (t1, t2, t3, t4, t5, t6, t7, t8)
    })
  }

  implicit def tuple9DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9) => (t1, t2, t3, t4, t5, t6, t7, t8, t9)
    })
  }

  implicit def tuple10DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10)
    })
  }

  implicit def tuple11DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)
    })
  }

  implicit def tuple12DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12)
    })
  }

  implicit def tuple13DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13)
    })
  }

  implicit def tuple14DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14)
    })
  }

  implicit def tuple15DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15)
    })
  }

  implicit def tuple16DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16)
    })
  }

  implicit def tuple17DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO, T17: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16], io[T17])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17)
    })
  }

  implicit def tuple18DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO, T17: Manifest: DataIO, T18: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16], io[T17], io[T18])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18)
    })
  }

  implicit def tuple19DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO, T17: Manifest: DataIO, T18: Manifest: DataIO, T19: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16], io[T17], io[T18], io[T19])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19)
    })
  }

  implicit def tuple20DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO, T17: Manifest: DataIO, T18: Manifest: DataIO, T19: Manifest: DataIO, T20: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16], io[T17], io[T18], io[T19], io[T20])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20)
    })
  }

  implicit def tuple21DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO, T17: Manifest: DataIO, T18: Manifest: DataIO, T19: Manifest: DataIO, T20: Manifest: DataIO, T21: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16], io[T17], io[T18], io[T19], io[T20], io[T21])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20, t21: T21) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21)
    })
  }

  implicit def tuple22DataIO[T1: Manifest: DataIO, T2: Manifest: DataIO, T3: Manifest: DataIO, T4: Manifest: DataIO, T5: Manifest: DataIO, T6: Manifest: DataIO, T7: Manifest: DataIO, T8: Manifest: DataIO, T9: Manifest: DataIO, T10: Manifest: DataIO, T11: Manifest: DataIO, T12: Manifest: DataIO, T13: Manifest: DataIO, T14: Manifest: DataIO, T15: Manifest: DataIO, T16: Manifest: DataIO, T17: Manifest: DataIO, T18: Manifest: DataIO, T19: Manifest: DataIO, T20: Manifest: DataIO, T21: Manifest: DataIO, T22: Manifest: DataIO] = {
    new TupleDataIO[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)](io[T1], io[T2], io[T3], io[T4], io[T5], io[T6], io[T7], io[T8], io[T9], io[T10], io[T11], io[T12], io[T13], io[T14], io[T15], io[T16], io[T17], io[T18], io[T19], io[T20], io[T21], io[T22])({
      case Array(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6, t7: T7, t8: T8, t9: T9, t10: T10, t11: T11, t12: T12, t13: T13, t14: T14, t15: T15, t16: T16, t17: T17, t18: T18, t19: T19, t20: T20, t21: T21, t22: T22) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22)
    })
  }
}
