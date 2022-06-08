package part3typesdatasets

import org.apache.spark.sql.functions.{array_contains, avg, col}
import org.apache.spark.sql.{DataFrame, Dataset, Encoders, SparkSession}

object Datasets extends App {

  val spark = SparkSession.builder()
    .appName("Datasets")
    .config("spark.master", "local")
    .getOrCreate()

  val numbersDF: DataFrame = spark.read
    .format("csv")
    .option("header", "true")
    .option("inferSchema", "true")
    .load("src/main/resources/data/numbers.csv")

  numbersDF.printSchema()

  // convert a DF to a Dataset
  implicit val intEncoder = Encoders.scalaInt
  val numbersDS: Dataset[Int] = numbersDF.as[Int]

  numbersDS.filter(_ < 100)

  // dataset of a complex type
  // 1 - define your case class
  case class Car(
                  Name: String,
                  Miles_per_Gallon: Option[Double],
                  Cylinders: Long,
                  Displacement: Double,
                  Horsepower: Option[Long],
                  Weight_in_lbs: Long,
                  Acceleration: Double,
                  Year: String,
                  Origin: String
                )

  // 2 - read the DF from the file
  def readDF(filename: String) = spark.read
    .option("inferSchema", "true")
    .json(s"src/main/resources/data/$filename")

  import spark.implicits._

  val carsDF = readDF("cars.json")
  // 4 - convert the DF to DS
  val carsDS = carsDF.as[Car]

  // DS collection functions
  numbersDS.filter(_ < 100).show()

  // map, flatMap, fold, reduce, for comprehensions
  val carNamesDS = carsDS.map(car => car.Name.toUpperCase())

  //  carNamesDS.show()

  /**
   * Exercises
   *
   * 1. Count how many cars we have
   * 2. Count how many POWERFUL cars we have (HP > 140)
   * 3. Average HP for the entire dataset
   *
   */

  println(carsDS.count())

  val carsDSNotNull = carsDS.flatMap(_.Horsepower)

  val carsAbove140HP = carsDSNotNull.filter(_ > 140).count()
  println(carsAbove140HP)

  val avgHP = carsDSNotNull.reduce(_ + _) / carsDSNotNull.count()
  println(avgHP)

  // also use the DF functions!
  carsDS.select(avg(col("Horsepower"))).show

  // Joins
  case class Guitar(id: Long, make: String, model: String, guitarType: String)

  case class GuitarPlayer(id: Long, name: String, guitars: Seq[Long], band: Long)

  case class Band(id: Long, name: String, hometown: String, year: Long)

  val guitarsDS = readDF("guitars.json").as[Guitar]
  val guitarPlayersDS = readDF("guitarPlayers.json").as[GuitarPlayer]
  val bandsDS = readDF("bands.json").as[Band]

  val guitarPlayerBandsDS = guitarPlayersDS.joinWith(bandsDS, guitarPlayersDS.col("band") === bandsDS.col("id"), "inner")
  //  guitarPlayerBandsDS.show()

  /**
   * Exercise: join the guitarsDS and guitarPlayerDS
   * (hint: use array_contains)
   */

  val guitarPlayerGuitarDS = guitarPlayersDS
    .joinWith(guitarsDS, array_contains(guitarPlayersDS.col("guitars"), guitarsDS.col("id")), "outer")

  //  guitarPlayerGuitarDS.show

  // Grouping
  val carsGroupedByOrigin = carsDS
    .groupByKey(_.Origin)
    .count()

  carsGroupedByOrigin.show

  // joins and groups are WIDE transformations, will involve SHUFFLE operations

}
