#!/bin/sh
SPARK_HOME=~/spark-1.4.1-bin-hadoop2.6

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

LS_HOME=$SCRIPTPATH/../

rm -rf $LS_HOME/data/output/ScalaTagCount.txt
rm -rf $LS_HOME/data/output/ScalaQuestionsByMonth.txt

(cd $LS_HOME; sbt package)

# run locally on all cores
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master local[*] $LS_HOME/target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file $LS_HOME/data/stackexchange/stackoverflow.com-Posts/Posts1m-tail.xml \
  --output-directory $LS_HOME/data/output 
