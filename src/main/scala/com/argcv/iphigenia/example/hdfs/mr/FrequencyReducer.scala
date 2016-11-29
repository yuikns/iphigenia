package com.argcv.iphigenia.example.hdfs.mr

import java.lang.{ Iterable => JIterable }

import org.apache.hadoop.io.{ IntWritable, Text }
import org.apache.hadoop.mapreduce.Reducer

import scala.collection.JavaConverters._

/**
 * @author yu
 */
class FrequencyReducer extends Reducer[Text, IntWritable, Text, IntWritable] {
  type Context = Reducer[Text, IntWritable, Text, IntWritable]#Context

  override protected def reduce(eventID: Text, counts: JIterable[IntWritable], context: Context): Unit = {
    val sum = counts.asScala.map(_.get()).sum
    context.write(eventID, new IntWritable(sum))
  }
}

object FrequencyReducer {
  def instance = new FrequencyReducer().getClass
}
