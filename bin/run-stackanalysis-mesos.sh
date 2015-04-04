#!/bin/sh
SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

LS_HOME=$SCRIPTPATH/../

. $LS_HOME/conf/mesos/mesos-env.sh

rm -rf $LS_HOME/data/output/ScalaTagCount.txt
rm -rf $LS_HOME/data/output/ScalaQuestionsByMonth.txt
hdfs dfs -rm -r hdfs://$HDFS_HOST/output/*

(cd $LS_HOME; sbt package)

# run stackanalysis 100k using mesos 
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master $MESOS_MASTER \
--conf spark.executor.uri=hdfs://$HDFS_HOST/spark/spark-1.3.0-bin-hadoop2.4.tgz \
--conf spark.mesos.coarse=true \
--conf spark.mesos.extra.cores=8 $LS_HOME/target/scala-2.10/learning-spark_2.10-0.1.0.jar \
--input-file hdfs://$HDFS_HOST/posts/Posts100k.xml \
--output-directory hdfs://$HDFS_HOST/output

#--output-directory file://$SCRIPTPATH/data/output
