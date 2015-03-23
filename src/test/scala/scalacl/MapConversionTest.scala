package scalacl

class MapConversionTest extends BaseTest {

  behavior of "map"

  it should "compute scalar function with int type" in context {
    implicit context =>
      val f = 0.2f

      val array = Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      val clArray: CLArray[Int] = array.cl

      val clResult = clArray.map((x: Int) => x * 2 * f)
      val result = array.map(x => x * 2 * f)

      result.toArray zip clResult.toArray foreach {
        case (r, cl) => r should equal(cl +- 0.001f)
      }
  }

}
