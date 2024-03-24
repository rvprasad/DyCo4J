# DyCo4J (Dynamic Collector for JVM)

This project provides instrumentation-based toolkit to collect dynamic information
about JVM based code.
 - [Utility Library](https://github.com/rvprasad/DyCo4J/tree/master/utility)
 - [Logging Library](https://github.com/rvprasad/DyCo4J/tree/master/logging)
 - [Instrumentation Tools](https://github.com/rvprasad/DyCo4J/tree/master/instrumentation)


## Requirements
- JDK 21+. We use the JDKs from [Azul Systems](https://www.azul.com/products/zulu/) provided via [SDKMan](https://sdkman.io/).


## Build
- To build the libraries, execute the following commands (in order).
    1. `./gradlew clean test jar` in _logging_ folder.
    2. `./gradlew clean test jar` in _utility_ folder.
    3. `./gradlew clean test jar` in _instrumentation_ folder.


## Use

To illustrate how to use the tools, we will trace the execution of
[Apache Ant 1.10.14](http://ant.apache.org/).  We will use the source bundle for
illustration as they will help illustrate both _entry_ and _instrumentation_
tools.

### Setup
1. Download the source code from [here](http://ant.apache.org/srcdownload.cgi).
2. Unpack the source bundle.  We will refer to _apache-ant-1.10.14_ folder as the
   _\<root>_ folder.
3. Open the terminal and change the folder to _\<root>_ folder.
4. Build a bootstrapping version of ant by executing `./bootstrap.sh` to
5. Run the tests by executing `./bootstrap/bin/ant test`.
6. Make note of the number of tests that were executed, passed, failed, and
   skipped along with the time take to run the tests.  This information is
   available in `build/testcases/reports/index.html`.  Here's a [snapshot](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-vanilla-summary.png)
   of the report.

**Note**
1. Following tests were executed on a Linux box with 3.8GHz Intel Core i7
   processor, 64GB RAM, and 2TB flash drive.
2. `misc/scripts/test-on-apache-ant.sh` automates the below tracings when
   executed in the _root_ folder. 

### Tracing the Tests
1. Open the terminal and change the folder to _\<root>_ folder.
2. Create a clean copy of ant and its tests by executing
   `./bootstrap/bin/ant clean build compile-tests`.
3. Execute `cd build`.
4. Make a copy of the compiled tests by executing `mv testcases orig-testcases`.
5. Instrument the tests by executing `java -jar
   <path to dyco4j-entry-X.Y.Z-cli.jar> --in-folder orig-testcases --out-folder
   testcases` with all the jars required by the tool in the same folder as
   _dyco4j-entry-1.1.0-cli.jar_.
6. Execute `cd testcases`.
7. Place the logging classes in the class path by unpacking logging library jar
   by executing `jar xvf <path to dyco4j-logging-X.Y.Z.jar>`.
8. Get back to the _\<root>_ folder and execute `./bootstrap/bin/ant test`.
   This will create `trace.*gz` files in _\<root>_ and in
   _\<root>/src/etc/testcases/taskdefs/_ folders. 

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
   dyco4j-internals-1.1.0-cli.jar.
6. Get back to the _\<root>_ folder and execute `./bootstrap/bin/ant test`.
   This will create `trace.*gz` files in _\<root>_ and in
   _\<root>/src/etc/testcases/taskdefs/_ folders.

### Performance
1. **Baseline**: Without any instrumentation, all tests were executed in [2m30s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-baseline-summary.png).
2. **Test only**: When test entries were logged, _2,204 events were logged into
   59 files (<1MB) in [2m19s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-test-entry-summary.png)._
3. **Default options**: When method entry and exit were logged,
   _704,002,578 events were logged into 59 files (~74MB) in [5m41s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-default-options-summary.png)._
4. **Method values and field and array access without values**: When method entry 
   and exit, method args, method return values, method calls, and field and array 
   access without values were logged, _5,223,800,802 events were logged into 59 
   files (6.7GB) in [49m43s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-method-values-field-array-access-without-values-summary.png)._
5. **Method values and field access with values**: When method entry
   and exit, method args, method return values, method calls, and field 
   access with values were logged, _3,089,752,509 events were logged into 59
   files (3.4GB) in [30m57s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-method-values-field-access-with-values-summary.png)._
6. **Method values and array access with values**: When method entry
   and exit, method args, method return values, method calls, and array
   access with values were logged, _4,478,498,717 events were logged into 59
   files (8.3GB) in [53m17s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-method-values-array-access-with-values-summary.png)._
7. **Full**: When method entry and exit, method args, method return values, method
   calls, and field and array access with values were logged, 
   _5,223,872,394 events were logged into 59 files (10GB) in [64m16s](https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-all-options-with-values-summary.png)._

**Note**
The instrumentation in Bzip related tests contributed most to execution times 
when field and array accesses were logged. Array accesses contributed more 
than 50% while field accesses contributed <20%.

## Known Issues
 - Due to the behavior of JVM that is enforced in response to
   #[8172282](http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8172282),
   exceptions raised in super constructor calls are not logged.  This
   limitation was tracked in issue
   [#38](https://github.com/rvprasad/DyCo4J/issues/38).


## Info for Developers
 - If you dive into the source of this project, then search for the strings
   "INFO", "FIXME", and "ASSUMPTION" to uncover various bits of information not
   captured elsewhere.
 - If you want to run _instrumentation_ tests using tools other than Gradle,
   then remember to passing `-ea
   -Dlogging.jar=../logging/build/libs/dyco4j-logging-X.Y.Z.jar` as a VM
   option.
 - If you want to add new tests, then refer to _CLITest_, _AbstractCLITest_,
   and _CLIClassPathConfigTest_ to understand how to set up and tear down
   artifacts.


## Attribution

Copyright (c) 2017, Venkatesh-Prasad Ranganath

Licensed under BSD 3-clause "New" or "Revised" License (https://choosealicense.com/licenses/bsd-3-clause/)

**Authors:** Venkatesh-Prasad Ranganath
