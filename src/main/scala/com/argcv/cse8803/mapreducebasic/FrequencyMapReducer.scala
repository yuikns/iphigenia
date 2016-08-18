package com.argcv.cse8803.mapreducebasic

import com.argcv.valhalla.console.ColorForConsole._
import com.argcv.valhalla.utils.Awakable
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat

/**
  * @author yu
  */
object FrequencyMapReducer extends Awakable {

  def main(args: Array[String]): Unit = {
    // create a hadoop job and set main class
    val job = Job.getInstance()
    job.setJarByClass(FrequencyMapReducer.getClass)
    job.setJobName("Frequency")

    // set the input & output path
    FileInputFormat.addInputPath(job, new Path(args.head))
    FileOutputFormat.setOutputPath(job, new Path(s"${args(1)}-${System.currentTimeMillis()}"))

    // set mapper & reducer
    job.setMapperClass(FrequencyMapper.instance)
    job.setReducerClass(FrequencyReducer.instance)

    // specify the type of the output
    job.setOutputKeyClass(new Text().getClass)
    job.setOutputValueClass(new IntWritable().getClass)

    // run
    logger.info(s"job finished, status [${if (job.waitForCompletion(true)) "OK".withColor(GREEN) else "FAILED".withColor(RED)}]")
  }

}
