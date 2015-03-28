#!/bin/sh
SPARK_HOME=/usr/local/spark

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")

rm -rf $SCRIPTPATH/data/output/ScalaTagCount.txt
rm -rf $SCRIPTPATH/data/output/ScalaQuestionsByMonth.txt

# run locally on all cores
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master local[*] target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file $SCRIPTPATH/data/stackexchange/stackoverflow.com-Posts/Posts100k.xml \
  --output-directory $SCRIPTPATH/data/output 
