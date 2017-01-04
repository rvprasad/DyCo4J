#Logging Library
 
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

In case of class and instance initialization methods, `o:<uninitThis>` value
will be logged instead of the object id when logging the receiver before the
receiver is initialized.

Logging library can be configured with the following properties specified in
_logging.properties_ file.
  - _traceFolder_ where the trace files should be written.
  - _bufferLength_ to be used during logging.
  
This properties file should be available as _dyco4j/logging/logging.properties_
on the classpath.
  
- Required Runtime Dependences:
    - [ASM](http://asm.ow2.org/) 5.2 
    - [ASM Commons](http://asm.ow2.org/) 5.2


