package com.argcv.iphigenia.example.hdfs.mr

import org.apache.hadoop.io.{ IntWritable, LongWritable, Text }
import org.apache.hadoop.mapreduce.Mapper

/**
 * @author yu
 */
class FrequencyMapper extends Mapper[LongWritable, Text, Text, IntWritable] {
  type Context = Mapper[LongWritable, Text, Text, IntWritable]#Context

  override def map(offset: LongWritable, lineText: Text, context: Context): Unit = {
    val line = lineText.toString
    val eventID: String = line.split(",")(1)
    context.write(new Text(eventID), FrequencyMapper.ONE)
  }
}

object FrequencyMapper {
  def instance = new FrequencyMapper().getClass

  lazy val ONE = new IntWritable(1)
}
