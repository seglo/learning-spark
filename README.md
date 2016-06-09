Sean's learning-spark project
=============================

This repo contains various Spark projects I've created to help learn spark for myself, teach others, present, and other useful information  I've accumulated.

## [Exactly Once Message Delivery with Kafka & Cassandra](exactlyonce/)

The [`exactlyonce`](exactlyonce/) project is a demonstration of implementing Exactly Once message delivery semantics with Spark Streaming, Kafka, and Cassandra.  Exactly Once semantics with Kafka isn't too difficult if designed upfront.  I implemented it just like how the [Kafka documentation recommends](http://kafka.apache.org/documentation.html#semantics): by storing the partition and offset with the data I'm processing and then manage the topic's offset myself on the event of failure/restart.  On startup it will query Cassandra for the last offset for each partition and then begin processing from the next offset.  To maintain Exactly Once across a whole distributed infrastructure takes some work as you have to manage partition offset each step of the way, but it can be done fairly easily if designed up front.


## [StackOverflow.com Analysis](stackanalysis/)

The [`stackanalysis`](stackanalysis/) project analyzes StackOverflow.com post data to discover insights in regards to Scala questions asked on the site.

This project accompanied a [presentation](http://rawgit.com/seglo/learning-spark/master/presentation/learning-spark.html) for the [Scala Toronto](https://meetup.com/scalator) meetup group in the winter of 2015.

## [GitHub Events Streaming](githubstream/)

The [`githubstream`](githubstream/) project consumes data directly from the public Github Events API and demonstrates some common streaming capabilities of Apache Spark.

This project accompanied a [presentation](https://cdn.rawgit.com/seglo/learning-spark/master/presentation/spark-streaming-in-action/index.html) for the [Scala Up North](https://www.scalaupnorth.ca) Scala conference in the fall of 2015.


