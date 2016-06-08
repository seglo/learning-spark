#!/bin/sh
SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

export YARN_CONF_DIR=$LS_HOME/conf/yarn
SPARK_HOME=~/spark-1.3.0-bin-hadoop2.4

LS_HOME=$SCRIPTPATH/../

rm -rf $LS_HOME/data/output/ScalaTagCount.txt
rm -rf $LS_HOME/data/output/ScalaQuestionsByMonth.txt

(cd $LS_HOME; sbt package)

# run stackanalysis 100k using yarn-client
# i had this working on one point, i switched gears to using mesos
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master yarn-client target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file hdfs://sandbox:9000/user/root/stackexchange/stackoverflow.com-Posts/Posts100k.xml
