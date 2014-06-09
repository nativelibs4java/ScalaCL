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

import org.bridj.{ Pointer, PointerIO }

object IntDataIO extends ScalarDataIO[Int](PointerIO.getIntInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getIntAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Int) =
    buffers(bufferOffset).setIntAtIndex(index, value)
}

object ShortDataIO extends ScalarDataIO[Short](PointerIO.getShortInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getShortAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Short) =
    buffers(bufferOffset).setShortAtIndex(index, value)
}

object ByteDataIO extends ScalarDataIO[Byte](PointerIO.getByteInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getByteAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Byte) =
    buffers(bufferOffset).setByteAtIndex(index, value)
}

object LongDataIO extends ScalarDataIO[Long](PointerIO.getLongInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getLongAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Long) =
    buffers(bufferOffset).setLongAtIndex(index, value)
}

object FloatDataIO extends ScalarDataIO[Float](PointerIO.getFloatInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getFloatAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Float) =
    buffers(bufferOffset).setFloatAtIndex(index, value)
}

object DoubleDataIO extends ScalarDataIO[Double](PointerIO.getDoubleInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getDoubleAtIndex(index)
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Double) =
    buffers(bufferOffset).setDoubleAtIndex(index, value)
}

object BooleanDataIO extends ScalarDataIO[Boolean](PointerIO.getBooleanInstance) {
  override def get(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int) =
    buffers(bufferOffset).getByteAtIndex(index) != 0
  override def set(index: Long, buffers: Array[Pointer[_]], bufferOffset: Int, value: Boolean) =
    buffers(bufferOffset).setByteAtIndex(index, (if (value) 1 else 0).asInstanceOf[Byte])
}

trait ScalarDataIOs {
  implicit val byteDataIO = ByteDataIO
  implicit val shortDataIO = ShortDataIO
  implicit val intDataIO = IntDataIO
  implicit val longDataIO = LongDataIO
  implicit val floatDataIO = FloatDataIO
  implicit val doubleDataIO = DoubleDataIO
  implicit val booleanDataIO = BooleanDataIO
}
