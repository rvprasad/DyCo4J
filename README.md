# DyCo4J

This project provides instrumentation-based tools to collect dynamic information
about JVM based code.

##Utility Library

This library contains functionality commonly used in program analysis efforts.  
- Required Runtime Dependences:
    - [Gson](https://github.com/google/gson) 2.8
        
_Note:_ This library may be moved out of this repository/project.  


##Logging Library
 
This library contains functionality to log JVM based program information.
  
The library writes log statements to trace files with names conforming to
`^trace.*.gz` regex.  The first line of a trace file will be the time when the
trace file was created.  An execution that involves this logging library can
generate multiple trace files; specifically, one trace file for each
_java.lang.Class_ instance of _Logger_ class.
  
Each log statement in a trace file is a comma separated list of values.  It
starts with the thread id of the logger followed by the log message and an 
optional message frequency (if greater than 1).  The log message will
conform to one of the following formats.
- method entry `en,<method>`
  - This message will be followed by the set of corresponding method 
    argument messages with the exception in case of constructors.
- method argument `ar,<index>,<value>`
- method call `ca,<method>,<call-site-id>`
  - call-site-id is local to the caller method. 
- method return `re,<value>`
  - There will be no value in case of return from void methods.
- method exception `xp,<value>`
- method exit `ex,<method>,(N|E)`
  - `N` and `E` denote normal and exceptional exit, respectively.
  - This message will be preceded by the corresponding method return or 
    exception message.
- array access `(GETA|PUTA),<index>,<array>,<value>`
- field access `(GETF|PUTF),<field>,<receiver>,<value>`
    
Each value (including array and receiver) will have one of the following
prefixes to identify its type.
- array `a:`
- boolean `b:`
- byte `y:`
- char `c:`
- double `d:`
- float `f:`
- int `i:`
- long `l:`
- Object `o:`
- short `h:`
- String `s:`
- Throwable `t:`
    
True, false, and null values will be represented as `f`, `t`, and `null`.

Logging library can be configured with the following properties specified in
_logging.properties_ file.
  - _traceFolder_ where the trace files should be written.
  - _bufferLength_ to be used during logging.
  
This properties file should be available as _dyco4j/logging/logging.properties_
on the classpath.
  
- Required Runtime Dependences:
    - [ASM](http://asm.ow2.org/) 5.2


##Instrumentation Tools
    
These tools instrument code of interest with logging statements (based on above
_logging_ library) to collect information.  
  
The **entry** tool is intended to instrument the entry points in the program of
interest.  The instrumentation adds code to log a _marker_ statement each time
an entry point is executed.  This statement includes the fully qualified name
(in JVM format) of the entry point.
  
Out of the box, it instruments entry points of test cases, i.e., methods whose
names match `^test.*` regex and methods annotated with _JUnit
(`org.junit.{Test,After,Before,AfterClass,BeforeClass}`)_ and _TestNG
(`org.testng.annotations.{Test,AfterTest,BeforeTest,AfterClass,BeforeClass,
AfterMethod,BeforeMethod}`)_ annotations. (Note: We have not tested the tools
with TestNG annotations.)  
  
The **internals** tool is intended to instrument the internals of the program
of interest.  The instrumentation adds code to log information about
- method entries including arguments,
- method exits including return value and thrown exception, 
- field reads and writes, and 
- array reads and writes.
    
To curb the size of the generated traces, this tool maps FQNs (in JVM format)
of classes, methods, and fields to short ids during instrumentation and writes
the mapping into _program_data.json_ file. The injected logging statements use
the short ids instead of FQNs.  So, to make sense of the data in the traces
generated from the execution of the instrumented program, it should be decoded
using _program_data.json_, which can be deserialized via
`utility:dyco4j.utility.ProgramData.loadData` method.  

In both tools, the scope of instrumentation can be configured via
`--method-name-regex` command-line option, i.e., only instrument methods with
matching unqualified name.
  
- Required Runtime Dependences:
    - [ASM](http://asm.ow2.org/) 5.2 
    - [ASM Commons](http://asm.ow2.org/) 5.2
    - [ASM Tree](http://asm.ow2.org/) 5.2
    - [Commons CLI](http://commons.apache.org/proper/commons-cli/) 1.3.1
    - [SLF4J API](http://www.slf4j.org/) 1.7.22
    - [Gson](https://github.com/google/gson) 2.8
    - [DyCO4J Logging](https://github.com/rvprasad/DyCo4J) 1.0
    - [DyCO4J Utility](https://github.com/rvprasad/DyCo4J) 1.0
- Optional Depednences:
    - [SLF4J Simple](http://www.slf4j.org/) 1.7.22
        
        
## Requirements
- [Java](http://www.oracle.com/technetwork/java/javase/%20downloads/index.html) 1.8


## Build 
- To build the libraries, execute the following commands.
    1. `./gradlew clean test jar` in _logging_ folder.
    2. `./gradlew clean test jar` in _utility_ folder.

- To build the instrumentation tool, build the libraries (above) and then 
  execute the following commands in _instrumentation_ folder.
    1. `./gradlew copyUtility copyLogging`
    2. `./gradlew clean test jar`
    

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
   <path to dyco4j-entry-1.0.0-cli.jar> --in-folder orig-testcases --out-folder 
   testcases` with all the jars required by the tool in the same folder as 
   dyco4j-entry-1.0.0-cli.jar.
6. Execute `cd testcases`.
7. Place the logging classes in the class path by unpacking logging library jar
   by executing `jar xvf <path to dyco4j-logging-1.0.0.jar>`.
8. Get back to the _\<root>_ folder and execute `bootstrap/bin/ant test`.  This
   will create `trace.*gz` files in _\<root>_ and in 
   _\<root>/src/etc/testcases/taskdefs/_ folders.  Here's a [snapshot]
   (https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-tests-instrumented-summary.png)
   of the report in which 1806 events were logged in under 3 minutes.
   
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
   <path to dyco4j-internals-1.0.0-cli.jar> --in-folder orig-classes 
   --out-folder classes --classpath-config classpath-config.txt` 
   with all the jars required by the tool in the same folder as 
   dyco4j-internals-1.0.0-cli.jar.
6. Get back to the _\<root>_ folder and execute `bootstrap/bin/ant test`.  This
   will create `trace.*gz` files in _\<root>_ and in 
   _\<root>/src/etc/testcases/taskdefs/_ folders.  Here's a [snapshot]
   (https://github.com/rvprasad/DyCo4J/blob/master/misc/images/ant-impl-default-options-instrumented-summary.png)
   of the report in which 613,187,959 events were logged in under 13 minutes.

For the performance curious souls, when all tracing options are enabled, 
**TBD** events were logged in under **TBD** minutes. Here's a [snapshot]
(https://github.com/rvprasad/DyCo4J/misc/images/ant-impl-all-options-instrumented-summary.png) 
of that report.


## Info for Developers

 - If you dive into the source of this project, then search for the strings 
   "INFO" and "ASSUMPTION" to uncover various bits of information not captured 
   elsewhere.
 - If you want to run _instrumentation_ tests using tools other than Gradle,
   then remember to add `-ea 
   -Dlogging.jar=../logging/build/libs/dyco4j-logging-0.9.jar` to VM options.
 - If you want to add new tests, then look at the flow in _CLITest_,
   _AbstractCLITest_, and _CLIClassPathConfigTest_ to understand how to set up
   and tear down artifacts.

