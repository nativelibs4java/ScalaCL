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

import collection.mutable.ArrayBuffer

import com.nativelibs4java.opencl.CLEvent
import com.nativelibs4java.opencl.MockEvent

class DefaultScheduledDataTest
    extends BaseTest
    with RuntimeUniverseTest {

  behavior of "DefaultScheduledData"

  private val data = new DefaultScheduledData {
    override val context: Context = null
  }
  private def isLocked = data.scheduleLock.isLocked

  private val e1 = new MockEvent(1)
  private val e2 = new MockEvent(2)
  private val e3 = new MockEvent(3)

  it should "reads" in {
    read(e1, Nil, null)
    read(e2, List(e1), null)
  }

  ignore should "writes" in {
    write(e1, Nil, null)
    write(e2, Nil, e1)
  }

  ignore should "read -> write -> read" in {
    read(e1, Nil, null)
    write(e2, List(e1), null)
    read(e3, Nil, e2)
  }

  ignore should "write -> read -> write" in {
    write(e1, Nil, null)
    read(e2, Nil, e1)
    write(e3, List(e2), e1)
  }

  private def read(event: CLEvent, expectReads: List[CLEvent], expectWrite: CLEvent) {
    val events = new ArrayBuffer[CLEvent]
    data.startRead(events)
    Option(expectWrite).toSeq should equal(events.toList)
    assert(isLocked)

    data.endRead(event)
    expectWrite should equal(data.dataWrite)
    (expectReads ++ Option(event)) should equal(data.dataReads.toList)
    assert(!isLocked)
  }

  private def write(event: CLEvent, expectReads: List[CLEvent], expectWrite: CLEvent) {
    val events = new ArrayBuffer[CLEvent]
    data.startWrite(events)
    (expectReads ++ Option(expectWrite)) should equal(events.toList)
    assert(isLocked)

    data.endWrite(event)
    event should equal(data.dataWrite)
    data.dataReads.toList should equal(Nil)
    assert(!isLocked)
  }
}
