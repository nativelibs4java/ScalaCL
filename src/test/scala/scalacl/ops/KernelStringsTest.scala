package scalacl
package ops
package test

import scalacl.impl.KernelDef

import org.junit._
import Assert._

import org.bridj._

class KernelStringsTest {

  @Test
  def test() {

    val factor = 1.3f

    val boolArray = Array(true, false)
    val intArray = Array(1, 2, 3)
    val int2Array = intArray.map(i => (i, i * 10))
    val int3Array = intArray.map(i => (i, i * 10, i * 100))
    val int4Array = intArray.map(i => (i, i * 10, i * 100, i * 1000))
    val int8Array = intArray.map(i => (i, i * 10, i * 100, i * 1000, i, i * 10, i * 100, i * 1000))
    val int16Array = intArray.map(i => (i, i * 10, i * 100, i * 1000, i, i * 10, i * 100, i * 1000, i, i * 10, i * 100, i * 1000, i, i * 10, i * 100, i * 1000))
    val shortArray = Array[Short](1, 2, 3)
    val longArray = Array[Long](1, 2, 3)
    val byteArray = Array[Byte](1, 2, 3)
    val floatArray = Array(1f, 2f, 3f)
    val doubleArray = Array(1.0, 2.0, 3.0)
    val stringArray = Array(1, 2, 3).map(_.toString)

    val boolValue = true
    val intValue = 1
    val shortValue: Short = 1
    val longValue: Long = 1
    val byteValue: Byte = 1
    val floatValue = 1f
    val doubleValue = 1.0
    val stringValue = "1"

    val kd: KernelDef = cl"""
          constant bool boolArray[] = $boolArray;
          constant int intArray[] = $intArray;
          constant int2 int2Array[] = $int2Array;
          constant int3 int3Array[] = $int3Array;
          constant int4 int4Array[] = $int4Array;
          constant int8 int8Array[] = $int8Array;
          constant int16 int16Array[] = $int16Array;
          constant short shortArray[] = $shortArray;
          constant long longArray[] = $longArray;
          constant char byteArray[] = $byteArray;
          constant float floatArray[] = $floatArray;
          constant double doubleArray[] = $doubleArray;
          //const constant char* stringArray[] = $stringArray;

          constant bool boolValue = $boolValue;
          constant int intValue = $intValue;
          constant short shortValue = $shortValue;
          constant long longValue = $longValue;
          constant char byteValue = $byteValue;
          constant float floatValue = $floatValue;
          constant double doubleValue = $doubleValue;
          constant char stringValue[] = $stringValue;

          kernel void f(global int *in, global int *out, size_t n) {
            size_t i = get_global_id(0);
            bool b = i > n;
            b = true;
            if (b) return;
            if (i > n) return;
            out[i] = (int) ($factor * in[i]);
          }
        """
    println(kd)
    assertEquals(kd.sources.trim, """
          constant bool boolArray[] = { true, false };
          constant int intArray[] = { 1, 2, 3 };
          constant int2 int2Array[] = { int2(1, 10), int2(2, 20), int2(3, 30) };
          constant int3 int3Array[] = { int3(1, 10, 100), int3(2, 20, 200), int3(3, 30, 300) };
          constant int4 int4Array[] = { int4(1, 10, 100, 1000), int4(2, 20, 200, 2000), int4(3, 30, 300, 3000) };
          constant int8 int8Array[] = { int8(1, 10, 100, 1000, 1, 10, 100, 1000), int8(2, 20, 200, 2000, 2, 20, 200, 2000), int8(3, 30, 300, 3000, 3, 30, 300, 3000) };
          constant int16 int16Array[] = { int16(1, 10, 100, 1000, 1, 10, 100, 1000, 1, 10, 100, 1000, 1, 10, 100, 1000), int16(2, 20, 200, 2000, 2, 20, 200, 2000, 2, 20, 200, 2000, 2, 20, 200, 2000), int16(3, 30, 300, 3000, 3, 30, 300, 3000, 3, 30, 300, 3000, 3, 30, 300, 3000) };
          constant short shortArray[] = { ((short) 1), ((short) 2), ((short) 3) };
          constant long longArray[] = { 1L, 2L, 3L };
          constant char byteArray[] = { ((char) 1), ((char) 2), ((char) 3) };
          constant float floatArray[] = { 1.0F, 2.0F, 3.0F };
          constant double doubleArray[] = { 1.0, 2.0, 3.0 };
          //const constant char* stringArray[] = { "1", "2", "3" };

          constant bool boolValue = true;
          constant int intValue = 1;
          constant short shortValue = ((short) 1);
          constant long longValue = 1L;
          constant char byteValue = ((char) 1);
          constant float floatValue = 1.0F;
          constant double doubleValue = 1.0;
          constant char stringValue[] = "1";

          kernel void f(global int *in, global int *out, size_t n) {
            size_t i = get_global_id(0);
            bool b = i > n;
            b = true;
            if (b) return;
            if (i > n) return;
            out[i] = (int) (1.3F * in[i]);
          }
        """.trim)
  }
}
