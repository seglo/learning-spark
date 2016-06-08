package com.seglo.learningspark.githubstream.util

import com.seglo.learningspark.githubstream.GitHubEvent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.{ConstantInputDStream, DStream}
import org.specs2.mutable.After

import scala.collection.mutable

class SparkTest[T](val seq: Seq[GitHubEvent])
                  (val fun: DStream[GitHubEvent] => DStream[T]) extends After {
  lazy val ssc = new StreamingContext("local", "test", Seconds(1))
  val rdd = ssc.sparkContext.makeRDD(seq)

  val stream = new ConstantInputDStream(ssc, rdd)

  val collector = mutable.MutableList[T]()

  fun(stream).foreachRDD(rdd => collector ++= rdd.collect())

  ssc.start()
  ssc.awaitTerminationOrTimeout(1000)

  def after = ssc.stop()
}

//class SparkTest[T,TO](data: Seq[T])(val fun: DStream[T] => DStream[TO]) extends After {
//
//  lazy val ssc = new StreamingContext("local", "test", Seconds(1))
//  val rdd = ssc.sparkContext.makeRDD(data)
//
//  val stream = new ConstantInputDStream(ssc, rdd)
//
//  try {
//    val r = fun(stream)
//    results(stream)
//    ssc.start()
//    ssc.awaitTerminationOrTimeout(1000)
//    r
//  }
//  finally {
//    ssc.stop()
//  }
//
//  def results(stream: DStream[T]) = {
//    val collector = mutable.MutableList[T]()
//    stream.foreachRDD(rdd => collector ++= rdd.collect())
//    collector
//  }
//}