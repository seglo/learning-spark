#!/bin/sh
MESOS_MASTER=mesos://10.173.40.36:5050
SPARK_HOME=~/spark-1.3.0-bin-hadoop2.4

# run stackanalysis 100k using mesos 
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master $MESOS_MASTER target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file hdfs://10.173.40.36/stackexchange/stackoverflow.com-Posts/Posts100k.xml
