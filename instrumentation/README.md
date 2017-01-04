#Instrumentation Tools
    
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
        
