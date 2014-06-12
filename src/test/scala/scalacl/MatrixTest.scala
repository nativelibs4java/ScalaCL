package scalacl
import org.junit._
import Assert._

class MatrixTest {

  case class Matrix(data: CLArray[Float], rows: Int, columns: Int)(implicit context: Context) {
    def this(rows: Int, columns: Int)(implicit context: Context) =
      this(new CLArray[Float](rows * columns), rows, columns)
    def this(n: Int)(implicit context: Context) =
      this(n, n)
  }

  def mult(a: Matrix, b: Matrix, out: Matrix)(implicit context: Context) = {
    assert(a.columns == b.rows)
    assert(a.rows == out.rows)
    assert(b.columns == out.columns)

    // TODO remove need for all of these:
    val aData = a.data
    val bData = b.data
    val outData = out.data
    val outColumns = out.columns
    val aRows = a.rows
    val aColumns = a.columns
    val bColumns = b.columns
    kernel {
      // This block will either be converted to an OpenCL kernel or cause compilation error
      // It captures out.data, a.data and b.data
      for (i <- 0 until aRows; j <- 0 until bColumns) {
        // TODO chain map and sum (to avoid creating a builder here !)
        // outData(i * outColumns + j) =
        //   (0 until aColumns).map(k => {
        //     aData(i * aColumns + k) * bData(k * bColumns + j)
        //   }).sum
        // var tot = 0f
        // for (k <- 0 until aColumns) {
        //   //tot = tot + aData(i * aColumns + k) * bData(k * bColumns + j)
        //   tot = 10
        // }
        // outData(i * outColumns + j) = tot
      }
    }
  }

  @Ignore
  @Test
  def testMatrix2() {
    implicit val context = Context.best

    val n = 10
    val out = new Matrix(n)
    val outData = out.data
    kernel {
      // This block will either be converted to an OpenCL kernel or cause compilation error
      // It captures out.data, a.data and b.data
      for (i <- 0 until 10; j <- 0 until 20) {
        // TODO chain map and sum (to avoid creating a builder here !)
        // outData(i * 30 + j) =
        //   (0 until 30).map(k => {
        //     aData(i * 30 + k) * bData(k * 30 + j)
        //   }).sum
        var tot = 0f
        for (k <- 0 until 30) {
          //tot = tot + aData(i * aColumns + k) * bData(k * bColumns + j)
          tot = 10000
        }
        outData(i * 10 + j) = tot
      }
    }
  }

  @Ignore
  @Test
  def testMatrix() {
    implicit val context = Context.best

    val n = 10
    val a = new Matrix(n)
    val b = new Matrix(n)
    val out = new Matrix(n)

    mult(a, b, out)

    println(out.data)
  }
}
