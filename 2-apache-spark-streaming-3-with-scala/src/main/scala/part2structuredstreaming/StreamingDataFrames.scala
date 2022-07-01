package part2structuredstreaming

import common.stocksSchema
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.concurrent.duration.DurationInt

object StreamingDataFrames {

  // reading a DF
  val spark = SparkSession.builder()
    .appName("Our first streams")
    .master("local[2]")
    .getOrCreate()

  // consuming a DF
  def readFromSocket() = {
    val lines: DataFrame = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 12345)
      .load()

    // transformations
    val shortLines = lines.filter(length(col("value")) <= 5)

    // tell between a static vs a streaming DF
    println(shortLines.isStreaming)

    val query = shortLines.writeStream
      .format("console")
      .outputMode("append")
      .start()

    // wait for the stream to finish
    query.awaitTermination()

  }

  def readFromFiles() = {
    val stocksDF: DataFrame = spark.readStream
      .format("csv")
      .option("header", "false")
      .option("dateFormat", "MMM d yyyy")
      .schema(stocksSchema)
      .load("src/main/resources/data/stocks")

    stocksDF.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()

  }

  def demoTriggers() = {
    val lines: DataFrame = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 12345)
      .load()

    // write the lines DF at a certain trigger
    lines.writeStream
      .format("console")
      .outputMode("append")
      .trigger(
//        Trigger.ProcessingTime(2.seconds)
//        Trigger.Once() // single batch, then terminate
        Trigger.Continuous(2.seconds) // experimental, every 2 seconds create a batch with whatever you have
      ) // every 2 seconds run the query
      .start()
      .awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    demoTriggers()
  }

}
