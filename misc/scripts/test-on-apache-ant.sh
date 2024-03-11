#!/bin/bash

# Copyright (c) 2016, Venkatesh-Prasad Ranganath
#
# BSD 3-clause License
#
# Author: Venkatesh-Prasad Ranganath (rvprasad)

# Script to test instrumentation on apache-ant

INSTRUMENTATION_VERSION=1.1.0
LOGGING_VERSION=1.1.0
DYCO4J=$HOME/workspace/dyco4j
LOGGING_JAR=$DYCO4J/logging/build/libs/dyco4j-logging-$LOGGING_VERSION.jar
ENTRY_JAR=$DYCO4J/instrumentation/build/libs/dyco4j-entry-$INSTRUMENTATION_VERSION-cli.jar
INTERNALS_JAR=$DYCO4J/instrumentation/build/libs/dyco4j-internals-$INSTRUMENTATION_VERSION-cli.jar

rm -rf traces

# bootstrapping ant
./bootstrap.sh
./bootstrap/bin/ant clean test
TRACE_FOLDER=traces/baseline
mkdir -p $TRACE_FOLDER
mv build/testcases/reports $TRACE_FOLDER


# test entry instrumentation
./bootstrap/bin/ant clean build compile-tests
cd build
mv {,orig-}testcases
java -jar $ENTRY_JAR --in-folder orig-testcases --out-folder testcases
cd testcases
jar xvf $LOGGING_JAR
cd ../../
./bootstrap/bin/ant test
TRACE_FOLDER=traces/entry-instr
mkdir -p $TRACE_FOLDER
mv trace*gz $TRACE_FOLDER
mv build/program_data.json $TRACE_FOLDER
find src -name "trace*gz" -exec mv '{}' $TRACE_FOLDER \;
echo "Logged `find $TRACE_FOLDER -name "trace*gz" -exec zcat '{}' \; | wc -l` statements"
echo "Files `ls -1 $TRACE_FOLDER/*gz | wc -l`"
echo "Size `du -msc $TRACE_FOLDER/*gz | grep total`"
mv build/testcases/reports $TRACE_FOLDER


# test implementation instrumentation with default options 
cd build
mv {,orig-}classes
find ../lib/optional -name "*jar" > classpath-config.txt
java -jar $INTERNALS_JAR --in-folder orig-classes --out-folder classes --classpath-config classpath-config.txt
cd ..
./bootstrap/bin/ant test
TRACE_FOLDER=traces/basic-instr
mkdir -p $TRACE_FOLDER
mv trace*gz $TRACE_FOLDER
mv build/program_data.json $TRACE_FOLDER
find src -name "trace*gz" -exec mv '{}' $TRACE_FOLDER \;
echo "Logged `find $TRACE_FOLDER -name "trace*gz" -exec zcat '{}' \; | wc -l` statements"
echo "Files `ls -1 $TRACE_FOLDER/*gz | wc -l`"
echo "Size `du -msc $TRACE_FOLDER/*gz | grep total`"
mv build/testcases/reports $TRACE_FOLDER


# test implementation instrumentation with all options except --trace-array-access
cd build
rm -rf classes
java -jar $INTERNALS_JAR --in-folder orig-classes --out-folder classes --classpath-config classpath-config.txt --trace-field-access --trace-method-arguments --trace-method-return-value --trace-method-call
cd ..
./bootstrap/bin/ant test
TRACE_FOLDER=traces/no-array-instr
mkdir -p $TRACE_FOLDER
mv trace*gz $TRACE_FOLDER
mv build/program_data.json $TRACE_FOLDER
find src -name "trace*gz" -exec mv '{}' $TRACE_FOLDER \;
echo "Logged `find $TRACE_FOLDER -name "trace*gz" -exec zcat '{}' \; | wc -l` statements"
echo "Files `ls -1 $TRACE_FOLDER/*gz | wc -l`"
echo "Size `du -msc $TRACE_FOLDER/*gz | grep total`"
mv build/testcases/reports $TRACE_FOLDER


# test implementation instrumentation with all options 
cd build
rm -rf classes
java -jar $INTERNALS_JAR --in-folder orig-classes --out-folder classes --classpath-config classpath-config.txt --trace-array-access --trace-field-access --trace-method-arguments --trace-method-return-value --trace-method-call
cd ..
./bootstrap/bin/ant test
TRACE_FOLDER=traces/full-instr
mkdir -p $TRACE_FOLDER
mv trace*gz $TRACE_FOLDER
mv build/program_data.json $TRACE_FOLDER
find src -name "trace*gz" -exec mv '{}' $TRACE_FOLDER \;
echo "Logged `find $TRACE_FOLDER -name "trace*gz" -exec zcat '{}' \; | wc -l` statements"
echo "Files `ls -1 $TRACE_FOLDER/*gz | wc -l`"
echo "Size `du -msc $TRACE_FOLDER/*gz | grep total`"
mv build/testcases/reports $TRACE_FOLDER

