# An introduction to Spark through demonstration

## [Presentation](http://rawgit.com/seglo/learning-spark/master/presentation/learning-spark.html)

Type `p` in presentation to see notes.

## Project & Demo

To use the full [StackOverflow.com dataset](http://blog.stackexchange.com/category/cc-wiki-dump/) you must download the [BitTorrent from the Internet Archive](https://archive.org/download/stackexchange/stackexchange_archive.torrent).

[More information about the Stack Exchange data dump.](https://archive.org/details/stackexchange)

### Local

I've bundled a 100k line sample of the StackOverflow.com posts data in this repository.  To run set the `SPARK_HOME` variable in the `run-stackanalysis-local.sh` script, package, and execute.

```bash
./run-stack-analysis-local.sh
```

To run this application over the whole dataset, update `input-file` in `run-stackanalysis-local.sh` to the real `stackoverflow.com-Posts/Posts.xml` file in the data dump.

The output will be put into `data/output` (see `output-directory` in `run-stackanalysis-local.sh` to change location.

### Cluster

In my presentation I used an Apache Mesos cluster [generated for me by Mesosphere](http://mesosphere.com/docs/getting-started/).  You can can take advantage of [Google Cloud Compute](https://cloud.google.com/compute/)'s deal where you can get $300 or 60 days of any of their services for free (28/03/15).

Once you've setup a cluster you can run the `StackAnalysis` Spark driver application on it by selecting the appropriate master.  Consult [Spark's documentation](https://spark.apache.org/docs/latest/) for more details on running Spark on various cluster technologies.

* [Run on Mesos cluster](https://spark.apache.org/docs/1.3.0/running-on-mesos.html)
* [Run on YARN cluster](https://spark.apache.org/docs/1.3.0/running-on-yarn.html)

i.e.) Making `StackAnalysis` accessible via `spark-shell`.

```bash
$SPARK_HOME/bin/spark-submit --master mesos://mesos-host:5050 --jars target/scala-2.10/learning-spark_2.10-0.1.0.jar
```

i.e.) Submitting on a Mesos cluster

**NOTE**: As of 28/03/15 [I can't submit `StackAnalysis` with `spark-submit`](http://stackoverflow.com/questions/29198522/cant-run-spark-submit-with-an-application-jar-on-a-mesos-cluster).  However, loading it in with `spark-shell` works.

```bash
sbt package
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master mesos://mesos-host:5050 target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file hdfs://mesos-host/stackexchange/stackoverflow.com-Posts/Posts100k.xml \
```

i.e.) Submitting on a YARN cluster

```bash
export YARN_CONF_DIR=conf
$SPARK_HOME/bin/spark-submit --class "StackAnalysis" --master yarn-client target/scala-2.10/learning-spark_2.10-0.1.0.jar \
  --input-file hdfs://sandbox:9000/user/root/stackexchange/stackoverflow.com-Posts/Posts100k.xml
```

## Output

### Scala Question count by Month
                         
```bash
$ cat data/output/ScalaQuestionsByMonth.txt/*
(2008-09,6)
(2008-10,8)
(2008-11,5)
(2008-12,3)
(2009-01,12)
(2009-02,12)
(2009-03,17)
(2009-04,27)
(2009-05,12)
(2009-06,64)
(2009-07,59)
(2009-08,71)
(2009-09,67)
(2009-10,101)
(2009-11,95)
(2009-12,84)
(2010-01,105)
(2010-02,115)
(2010-03,115)
(2010-04,163)
(2010-05,169)
(2010-06,176)
(2010-07,185)
(2010-08,170)
(2010-09,232)
(2010-10,218)
(2010-11,212)
(2010-12,228)
(2011-01,254)
(2011-02,274)
(2011-03,301)
(2011-04,275)
(2011-05,329)
(2011-06,336)
(2011-07,427)
(2011-08,436)
(2011-09,421)
(2011-10,470)
(2011-11,458)
(2011-12,376)
(2012-01,361)
(2012-02,393)
(2012-03,463)
(2012-04,405)
(2012-05,424)
(2012-06,446)
(2012-07,490)
(2012-08,464)
(2012-09,535)
(2012-10,540)
(2012-11,554)
(2012-12,482)
(2013-01,579)
(2013-02,594)
(2013-03,671)
(2013-04,690)
(2013-05,661)
(2013-06,693)
(2013-07,706)
(2013-08,691)
(2013-09,796)
(2013-10,872)
(2013-11,873)
(2013-12,842)
(2014-01,794)
(2014-02,880)
(2014-03,986)
(2014-04,973)
(2014-05,883)
(2014-06,975)
(2014-07,950)
(2014-08,922)
(2014-09,398)
```

### Top Co-Occuring Scala Tags

[Complete list](https://raw.githubusercontent.com/seglo/learning-spark/master/data/output/ScalaTagCount-complete.txt)

```bash
$ cat data/output/ScalaTagCount.txt/* | head -n 100
(2687,java)
(2205,playframework)
(1972,sbt)
(1846,playframework-2.0)
(1309,akka)
(884,lift)
(851,functional-programming)
(669,types)
(665,actor)
(650,scala-collections)
(639,json)
(544,intellij-idea)
(532,slick)
(530,generics)
(462,eclipse)
(453,pattern-matching)
(437,scala-2.10)
(418,reflection)
(376,playframework-2.1)
(374,mongodb)
(367,playframework-2.2)
(362,list)
(360,scalaz)
(346,scala-2.8)
(336,collections)
(318,xml)
(318,map)
(318,implicit)
(297,future)
(290,scalatest)
(263,maven)
(261,parsing)
(255,spray)
(250,apache-spark)
(240,implicit-conversion)
(237,android)
(236,concurrency)
(223,regex)
(220,case-class)
(218,scala-macros)
(214,specs2)
(198,jvm)
(191,arrays)
(191,performance)
(188,haskell)
(186,inheritance)
(182,swing)
(170,type-inference)
(170,scala-java-interop)
(168,unit-testing)
(168,scala-ide)
(167,traits)
(167,recursion)
(166,macros)
(166,trait)
(165,shapeless)
(165,function)
(164,anorm)
(159,immutability)
(158,multithreading)
(158,monads)
(157,casbah)
(157,parser-combinators)
(151,scalatra)
(151,string)
(143,constructor)
(143,testing)
(143,tuples)
(142,serialization)
(141,syntax)
(134,class)
(129,javascript)
(124,read-eval-print-loop)
(121,parallel-processing)
(121,squeryl)
(115,spring)
(114,algorithm)
(112,mysql)
(107,compiler)
(105,typeclass)
(103,hadoop)
(103,stream)
(100,lazy-evaluation)
(99,type-erasure)
(98,reactivemongo)
(98,clojure)
(97,forms)
(97,database)
(96,scala-2.9)
(96,templates)
(95,for-comprehension)
(94,oop)
(93,iterator)
(93,asynchronous)
(92,rest)
(91,sql)
(91,python)
(90,type-parameter)
(90,dsl)
(90,postgresql)
```

## References

### Spark
* [Spark Documentation](https://spark.apache.org/docs/latest/)
* [Spark: Cluster Computing with Working Sets](https://amplab.cs.berkeley.edu/wp-content/uploads/2011/06/Spark-Cluster-Computing-with-Working-Sets.pdf) - University of Berkely paper introducing Spark - Much of the introduction of this talk is paraphrased from this paper.
* **[`spark-workshop` by Dean Wampler](https://github.com/deanwampler/spark-workshop) - Spark impl. examples in a Typesafe activator project**
* [Cloudera: Introduction to Apache Spark developer training](http://www.slideshare.net/cloudera/spark-devwebinarslides-final?related=1)

### Clusters

* [Google Cloud Compute](https://cloud.google.com/compute/)
* [Mesosphere in the Cloud](http://mesosphere.com/docs/getting-started/) - Tutorials to setup mesosphere on IaaS (Amazon, DigitalOcean, Google)
  * [Google Cloud Compute setup](http://mesosphere.com/docs/getting-started/cloud/google/)
  * [Running Spark on Mesos](http://spark.apache.org/docs/1.3.0/running-on-mesos.html) - Mesosphere docs are out-of-date, use this once mesosphere sets up cluster for you.
* [Spark atop Mesos on Google Cloud Platform](http://ceteri.blogspot.ca/2014/09/spark-atop-mesos-on-google-cloud.html)
* [Running Spark on Mesos](https://spark.apache.org/docs/1.3.0/running-on-mesos.html)
* [YARN Architecture](http://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/YARN.html)
* [HDFS Architecture](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html)
* [`docker-spark` by SequenceIQ](https://github.com/sequenceiq/docker-spark) - If you want to play around with a YARN cluster config

### Data

* [Stack Exchange Creative Commons data](http://blog.stackexchange.com/category/cc-wiki-dump/)