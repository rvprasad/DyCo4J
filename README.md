# DyCo4J

This project provides instrumentation-based toolkit to collect dynamic information
about JVM based code.
 - [Utility Library](https://github.com/rvprasad/DyCo4J/tree/master/utility)
 - [Logging Library](https://github.com/rvprasad/DyCo4J/tree/master/logging)
 - [Instrumentation Tools](https://github.com/rvprasad/DyCo4J/tree/master/instrumentation)


## Requirements
- JDK 1.8+. We use the JDKs from the following sources.
  - [Oracle](http://www.oracle.com/technetwork/java/javase/%20downloads/index.html)
  - [Azul Systems](https://www.azul.com/products/zulu/)


## Build
- To build the libraries, execute the following commands (in order).
    1. `./gradlew clean test jar` in _logging_ folder.
    2. `./gradlew clean test jar` in _utility_ folder.
    3. `./gradlew clean test jar` in _instrumentation_ folder.


## Use

To illustrate how to use the tools, we will trace the execution of
[Apache Ant 1.9.7](http://ant.apache.org/).  We will use the source bundle for
illustration as they will help illustrate both _entry_ and _instrumentation_
tools.

### Setup
1. Download the source code from [here](http://ant.apache.org/srcdownload.cgi).
2. Unpack the source bundle.  We will refer to _apache-ant-1.9.7_ folder as the
   _\<root>_ folder.
3. Open the terminal and change the folder to _\<root>_ folder.
4. Build an bootstrapping version of ant by executing `bootstrap.sh` to
5. Run the tests by executing `bootstrap/bin/ant test`.
6. Make note of the number of tests that were executed, passed, failed, and
   skipped along with the time take to run the tests.  This information is
   available in `build/testcases/reports/index.html`.  Here's a [snapshot]
   (https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-vanilla-summary.png)
   of the report.

### Tracing the Tests
1. Open the terminal and change the folder to _\<root>_ folder.
2. Create a clean copy of ant and its tests by executing
   `bootstrap/bin/ant clean build compile-tests`.
3. Execute `cd build`.
4. Make a copy of the compiled tests by executing `mv testcases orig-testcases`.
5. Instrument the tests by executing `java -jar
   <path to dyco4j-entry-X.Y.Z-cli.jar> --in-folder orig-testcases --out-folder
   testcases` with all the jars required by the tool in the same folder as
   _dyco4j-entry-1.0.0-cli.jar_.
6. Execute `cd testcases`.
7. Place the logging classes in the class path by unpacking logging library jar
   by executing `jar xvf <path to dyco4j-logging-X.Y.Z.jar>`.
8. Get back to the _\<root>_ folder and execute `bootstrap/bin/ant test`.  This
   will create `trace.*gz` files in _\<root>_ and in
   _\<root>/src/etc/testcases/taskdefs/_ folders.  Here's a [snapshot](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-entry-instrumented-summary.png)
   of the report in which 1,887 events in 81 files (336KB) were logged in under
   3 minutes.

### Tracing the Implementation (Internals)
1. Perform steps 1-7 from _Tracing the Tests_.  If you performed step 8, then
   make sure you delete old trace files.
2. Execute `cd build`.
3. Make a copy of the compiled implementation classes by executing `mv classes
   orig-classes`.
4. Create _classpath-config.txt_ file with paths of the dependent jars for the
   implementation available under _\<root>/lib/optional_ folder.  Place one
   path per line.  To avoid hassle, use absolute paths.
5. Instrument the implementation by executing `java -jar
   <path to dyco4j-internals-X.Y.Z-cli.jar> --in-folder orig-classes
   --out-folder classes --classpath-config classpath-config.txt`
   with all the jars required by the tool in the same folder as
   dyco4j-internals-1.0.0-cli.jar.
6. Get back to the _\<root>_ folder and execute `bootstrap/bin/ant test`.  This
   will create `trace.*gz` files in _\<root>_ and in
   _\<root>/src/etc/testcases/taskdefs/_ folders.  Here's a [snapshot]
   (https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-impl-default-options-instrumented-summary.png)
   of the report in which _613,017,601 events in 81 files (55MB)
   were logged in under 7 minutes._

For the performance curious peeps,
 - The baseline time for building and executing all tests on ant without any
   instrumentation was under 3 minutes. Here's a [snapshot](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-vanilla-summary.png).
 - When all tracing options were enabled, _4,848,544,012 events in 81 files
   (9.7GB) were logged in under 96 minutes._ Here's a [snapshot](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-impl-all-options-instrumented-summary.png)
   of that report.  Interestingly, Bzip related tests contributed the largest
   increase to execution time (~66 minutes).
 - When all tracing options except `--trace-array-access` were enabled
   _2,715,652,923 events in 80 files (3.1GB) amounting to  were logged in
   under 40 minutes._ Here's a [snapshot](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-impl-all-but-no-array-access-option-instrumented-summary.png)
   of that report.


## Known Issues
 - Due to a bug #[8172282](http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8172282)
   in JDK implementations (from both Oracle and Azul Systems), exceptions
   raised in super constructor calls are not logged.  This limitation is
   captured as issue [#38](https://github.com/rvprasad/DyCo4J/issues/38). It
   will be fixed once the JDK bug is addressed.


## Info for Developers
 - If you dive into the source of this project, then search for the strings
   "INFO", "FIXME", and "ASSUMPTION" to uncover various bits of information not
   captured elsewhere.
 - If you want to run _instrumentation_ tests using tools other than Gradle,
   then remember to add `-ea
   -Dlogging.jar=../logging/build/libs/dyco4j-logging-X.Y.Z.jar` to VM
   options.
 - If you want to add new tests, then look at the flow in _CLITest_,
   _AbstractCLITest_, and _CLIClassPathConfigTest_ to understand how to set up
   and tear down artifacts.


## Attribution

Copyright (c) 2017, Venkatesh-Prasad Ranganath

Licensed under BSD 3-clause "New" or "Revised" License (https://choosealicense.com/licenses/bsd-3-clause/)

**Authors:** Venkatesh-Prasad Ranganath
