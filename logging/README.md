# Logging Library

This library contains functionality to log JVM based program information.

The library writes log statements to trace files with names conforming to
`^trace.*.gz` regex.  The first line of a trace file will be the time when the
trace file was created.  An execution that involves this logging library can
generate multiple trace files; specifically, one trace file for each
_java.lang.Class_ instance of _Logger_ class.

Each log statement in a trace file is a comma separated list of values.  It
starts with the thread id of the logger followed by the log message and an
optional message frequency (if greater than 1).  Log messages will conform
to one of the following formats.
- method entry `en,<method>`
  - When method arguments are logged, this message will be followed by a 
    set of log messages corresponding to the method's arguments except in 
    case of constructors.
- method argument `ar,<index>,<value>`
- method call `ca,<method>,<call-site-id>`
  - call-site-id is local to the caller method.
- method return `re,<value>`
  - In case of void methods, there will be no \<value\>.
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

True, false, and null values are represented as `f`, `t`, and `null`.

When field and array access are logged without values, `*` is logged instead 
of values.

In case of class and instance initialization methods, when logging the 
receiver before the receiver is initialized, `o:<uninitThis>` value 
will be logged instead of the receiver's object id .

Logging library can be configured with the following properties specified in
_logging.properties_ file.
  - _traceFolder_ where the trace files should be written.
  - _bufferLength_ to be used during logging.

This file should be available as _dyco4j/logging/logging.properties_ on the
classpath.

- Required Runtime Dependences:
    - [ASM](http://asm.ow2.org/) 9.6
    - [ASM Commons](http://asm.ow2.org/) 9.6


## Attribution

Copyright (c) 2017, Venkatesh-Prasad Ranganath

Licensed under BSD 3-clause "New" or "Revised" License (https://choosealicense.com/licenses/bsd-3-clause/)

**Authors:** Venkatesh-Prasad Ranganath
