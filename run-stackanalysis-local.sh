#!/bin/sh
SPARK_HOME=/usr/local/spark

# todo: construct hdfs file format to make relative 

# run locally on 8 cores
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master local[*] target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file file:///home/seglo/source/learning-spark/data/stackexchange/stackoverflow.com-Posts/Posts1m.xml \
  --output-file /home/seglo/source/learning-spark/data/output/StackAnalysisCount.txt
#  --output-file file:///home/seglo/source/learning-spark/data/output/StackAnalysisCount.txt
# file:///home/seglo/source/learning-spark/data/stackexchange/stackoverflow.com-Posts/Posts1m.xml 
# file:///home/seglo/source/learning-spark/data/stackexchange/stackoverflow.com-Posts/Posts.xml
