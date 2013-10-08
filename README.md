ScalaCL... v3 (yeah, yet another rewrite from scratch FTW!)

NOT FUNCTIONAL YET, WORK IN PROGRESS (see [ScalaCL](https://code.google.com/p/scalacl/) if you want something that _works_, albeit only on Scala 2.9.x).

Features of the new design:
- Much better asynchronicity support (now requires OpenCL 1.1), and much better performance in general
- Support for captures of constants *and* OpenCL arrays
- Support for lazy clones for fast zipping
- Kernels are now fully specialized on static types and generated at compile-time (allows much faster startup and caching at runtime)
- ScalaCL Collections no longer fit in regular Scala Collections, to avoid silent data transfers / conversions when using unaccelerated methods (syntax stays the same, though)
- No more CLRange: expecting compiler to do its job

TODO:
- Finish Scalaxy/Reified integration (started under CLFunc / CLFuncUtils)
- Add more tests: DataIO, CodeConversion, scheduling, uniqueness / caching of kernels
- Implement missing DataIO[T], support case classes as tuples
- Catch up with compiler plugin:
  - Auto-vectorization
     - 1D works
     - Add 2D
     - add filters
  - Import Scalaxy streams, make them work with scala.reflection.api.Universe
- Plug some v2 runtime code back (filtered array compaction, reduceSymmetric, parallel sums...)
- Benchmarks!

Example that will eventually work:

    import scalacl._
    
    case class Matrix(data: CLArray[Float], rows: Int, columns: Int)(implicit context: Context) {
      def this(rows: Int, columns: Int) =
        this(new CLArray[Float](rows * columns), rows, columns)
      def this(n: Int) =
        this(n, n)
        
      def putProduct(a: Matrix, b: Matrix): Unit = {
        assert(a.columns == b.rows)
        assert(a.rows == rows)
        assert(b.columns == columns)
        
        kernel {
          // This block will either be converted to an OpenCL kernel or cause compilation error
		  for (i <- 0 until rows; j <- 0 until columns) {
		    data(i * columns + j) = (0 until a.columns).map(k => {
		      a.data(i * a.columns + k) * b.data(k * b.columns + j)
		    }).sum
		  }
	    }
      }
      
      def putSum(a: Matrix, b: Matrix): Unit = {
        assert(a.columns == b.columns && a.columns == columns)
        assert(a.rows == b.rows && a.rows == rows)
        
        kernel {
          for (i <- 0 until rows; j <- 0 until columns) {
          	val offset = i * columns + j
		    data(offset) = a.data(offset) + b.data(offset)
		  }
	    }
      }
    }
            
    implicit val context = Context.best

    val n = 10
    val a = new Matrix(n)
    val b = new Matrix(n)
    val out = new Matrix(n)
    
    out.putProduct(a, b)
    
    println(out.data)
