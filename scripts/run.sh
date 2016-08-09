#!/bin/sh
MY_JAVA_OPTS="-Xms6g -Xmx6g -XX:+UseConcMarkSweepGC -XX:ParallelCMSThreads=16 -XX:ParallelGCThreads=16 "
env JAVA_OPTS="$MY_JAVA_OPTS" sbt clean compile run

