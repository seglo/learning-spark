#!/bin/sh
SPARK_HOME=/usr/local/spark

$SPARK_HOME/bin/spark-shell --jars target/scala-2.10/learning-spark_2.10-0.1.0.jar
