Here are ideas of tasks to undertake (please contact the main author before picking them up: this list might be outdated!)

  * Fix ignored / broken tests
  * Add more tests: tests that pass + tests that break but should pass.
    Look at https://github.com/ochafik/nativelibs4java/tree/master/libraries/OpenCL/Core/src/test/java/com/nativelibs4java/opencl/ for ideas of tests.
  * Add a performance test (ScalaCL CPU vs. ScalaCL GPU vs. simple Array-based code vs. Parallel Array-based code).
    Look at https://github.com/ochafik/nativelibs4java/blob/master/libraries/BridJ/src/test/java/org/bridj/ComparisonTest.java for an example.
    
Survival guide:

  * Fork ScalaCL and Scalaxy on Github and clone them to `$BASE/ScalaCL` and `$BASE/Scalaxy`.
  * Always work in ScalaCL: it will pick up Scalaxy as a dependent project.
  * Code is automatically reformated upon `sbt compile` or manually with `sbt scalariform-format`
  * Any VM native crash should be reported to BridJ project.
