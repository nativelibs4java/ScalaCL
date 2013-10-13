package scalacl
package impl

import scala.reflect.runtime.universe.{ typeTag, TypeTag }

/**
 * Getter takes input and output name
 */
case class InlinedSet(defs: String, getter: (String, String) => String, size: String)

/**
 * Getter takes input, output name and presence output name.
 */
case class InlinedMap(defs: String, getter: (String, String, String) => String, size: String)

object InlinedCollections {
  def convertType[A: TypeTag]: String = typeTag[A].tpe.toString.toLowerCase

  def inline(v: Any): String = v.toString

  def inlineMapAsSwitch[A: TypeTag, B: TypeTag](mapName: String, map: Map[A, B]): String = {
    implicit val comparator = new java.util.Comparator[Comparable[_]] {
      override def compare(a: java.lang.Comparable[_], b: java.lang.Comparable[_]): Int = {
        a.asInstanceOf[Comparable[AnyRef]].compareTo(b.asInstanceOf[AnyRef])
      }
    }
    val cases = for (key <- map.keys.map(_.asInstanceOf[java.lang.Comparable[_]]).toSeq.sorted) yield {
      val value = map(key.asInstanceOf[A])
      s"""case ${inline(key)}: *value = ${inline(value)}; break;"""
    }

    s"""
      bool $mapName(const ${convertType[A]} key, const ${convertType[A]} *value) {
        switch (key) {
          ${cases.mkString("\n          ")}
          default:
            return false;
        }
        return true;
      }
    """
  }
}
