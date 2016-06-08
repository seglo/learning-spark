#!/bin/sh
SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

LS_HOME=$SCRIPTPATH/../

. $LS_HOME/conf/mesos/mesos-env.sh

(cd $LS_HOME; sbt package)

$SPARK_HOME/bin/spark-shell --master $MESOS_MASTER --jars $LS_HOME/target/scala-2.10/learning-spark_2.10-0.1.0.jar
