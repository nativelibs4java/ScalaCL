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

import com.nativelibs4java.opencl.CLContext
import com.nativelibs4java.opencl.CLDevice
import com.nativelibs4java.opencl.CLEvent
import com.nativelibs4java.opencl.CLKernel
import com.nativelibs4java.opencl.CLPlatform
import com.nativelibs4java.opencl.CLQueue
import com.nativelibs4java.opencl.JavaCL

import scalacl.impl.ConcurrentCache
import scalacl.impl.KernelDef
import scalacl.impl.ScheduledData

import scala.collection.mutable.ArrayBuffer

/**
 * ScalaCL context, which gathers an OpenCL context and a command queue.
 */
class Context(
    val context: CLContext,
    val queue: CLQueue) {

  private[this] val kernels = new ConcurrentCache[KernelDef, CLKernel]

  def getOrRegisterKernel(key: KernelDef)(initialValue: => CLKernel): CLKernel =
    kernels(key, _.release())(initialValue)

  def release() {
    queue.finish()
    queue.release()

    kernels.clear(_.release)
    context.release()
  }

  def waitFor(eventsToWaitFor: Seq[CLEvent]) =
    CLEvent.waitFor(eventsToWaitFor: _*)

  def schedule[S1 <: ScheduledData, S2 <: ScheduledData](
    inputs: Array[S1],
    outputs: Array[S2],
    operation: Array[CLEvent] => CLEvent): CLEvent = {

    val nData =
      (if (inputs != null) inputs.length else 0) +
        (if (outputs != null) outputs.length else 0)
    val eventsToWaitFor = new ArrayBuffer[CLEvent](nData)

    if (inputs != null)
      inputs.foreach(_.startRead(eventsToWaitFor))
    if (outputs != null)
      outputs.foreach(_.startWrite(eventsToWaitFor))

    var event: CLEvent = null
    try {
      event = operation(eventsToWaitFor.toArray)
      if (event != null)
        event.setCompletionCallback(new CLEvent.EventCallback {
          override def callback(status: Int) = {
            //          println("completed")
            if (inputs != null)
              inputs.foreach(_.eventCompleted(event))
            if (outputs != null)
              outputs.foreach(_.eventCompleted(event))
          }
        })
      event
    } finally {
      if (inputs != null)
        inputs.foreach(_.endRead(event))
      if (outputs != null)
        outputs.foreach(_.endWrite(event))
    }
  }
}

object Context {
  def best = {
    val context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU)
    val queue = context.createDefaultOutOfOrderQueueIfPossible
    //println("queue: " + queue + " (" + queue.getProperties + ")")
    new Context(context, queue)
  }
}
