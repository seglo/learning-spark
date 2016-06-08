#!/bin/sh
MASTER_HOST=development-5159-d9
MESOS_MASTER=mesos://$MASTER_HOST:5050
SPARK_HOME=~/spark-1.3.0-bin-hadoop2.4
HDFS_HOST=$MASTER_HOST

# this typically lives in SPARK_HOME/conf, but i moved it here so it's all in one spot
export MESOS_NATIVE_JAVA_LIBRARY=/usr/local/lib/libmesos.so
export SPARK_EXECUTOR_URI=hdfs://$HDFS_HOST/spark/spark-1.3.0-bin-hadoop2.4.tgz
