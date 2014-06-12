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
import impl._

import org.junit._
import Assert._

class KernelTest {
  @Test
  def intRange1DKernel() {
    implicit val context = Context.best
    try {

      val n = 25
      val a = new Array[Int](n)
      val ca = new CLArray[Int](n)
      val f = 10

      kernel {
        for (i <- 0 until n by 3)
          ca(i) = i * f + 10
      }
      for (i <- 0 until n by 3)
        a(i) = i * f + 10

      assertEquals(a.toSeq, ca.toSeq)
    } finally {
      context.release()
    }
  }
  @Test
  def longRange1DKernel() {
    implicit val context = Context.best
    try {

      val n = 25L
      val a = new Array[Long](n.toInt)
      val ca = new CLArray[Long](n)
      val f = 10

      kernel {
        for (i <- 0L until n by 3L)
          ca(i) = i * f + 10
      }
      for (i <- 0L until n by 3L)
        a(i.toInt) = i * f + 10

      assertEquals(a.toSeq, ca.toSeq)
    } finally {
      context.release()
    }
  }

  @Test
  def testEquality() {
    val sources = "aa"
    same(new KernelDef(sources = sources, salt = 1), new KernelDef(sources = sources, salt = 1))
    diff(new KernelDef(sources = sources, salt = 1), new KernelDef(sources = sources, salt = 2), sameHC = false)
    diff(new KernelDef(sources = sources, salt = 1), new KernelDef(sources = "a" + ('b' - 1), salt = 1), sameHC = true)
  }

  def same(a: AnyRef, b: AnyRef) = {
    assertEquals(a.hashCode, b.hashCode)
    assertEquals(a, b)
  }

  def diff(a: AnyRef, b: AnyRef, sameHC: Boolean) = {
    assertTrue(sameHC ^ (a.hashCode != b.hashCode))
    assertFalse(a.equals(b))
  }
}
