#!/bin/sh
export YARN_CONF_DIR=conf
SPARK_HOME=/usr/local/spark
# run stackanalysis 100k using yarn-client
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master yarn-client target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file hdfs://sandbox:9000/user/root/stackexchange/stackoverflow.com-Posts/Posts100k.xml
