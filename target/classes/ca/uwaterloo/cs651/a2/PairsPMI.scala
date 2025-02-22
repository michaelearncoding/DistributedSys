package ca.uwaterloo.cs651.a2; // Package declaration

import org.apache.spark.SparkContext  // Core Spark functionality
import org.apache.spark.SparkConf // Spark configuration
import org.rogach.scallop._ // Command line argument parsing
import scala.collection.mutable.HashMap // Mutable hash map collection
// import io.bespin.scala.util.Tokenizer  // Added missing import
import io.bespin.java.util.Tokenizer
import scala.collection.JavaConverters._ // Add Java to Scala conversion


// Main required options
// Serializable configuration case class
case class PMIConfig(
                      inputPath: String,
                      outputPath: String,
                      numReducers: Int,
                      threshold: Int
                    )

// Command line argument parsing
class Conf(args: Array[String]) extends ScallopConf(args) {
    mainOptions = Seq(input, output, reducers)
    val input = opt[String](required = true)
    val output = opt[String](required = true)
    val reducers = opt[Int](required = true)
    val threshold = opt[Int](default = Some(10))
    verify()
  }

  object PairsPMI {
    def main(args: Array[String]): Unit = {
      // Parse arguments and create configs
      val scallop = new Conf(args)
      val config = PMIConfig(
        scallop.input(),
        scallop.output(),
        scallop.reducers(),
        scallop.threshold()
      )

      val sc = new SparkContext(new SparkConf().setAppName("PairsPMI"))  // Create Spark context

    // First pass - count lines and words
    // First Pass - Data Processing
    val lines = sc.textFile(conf.input())  // Read input file
    //// RDD partitioned by default

    // Each worker processes its partition
    try {
        // First pass - process input and count lines
        val lines = sc.textFile(config.inputPath)
        val tokenized = lines.map(line => {
        val tokens = Tokenizer.tokenize(line).asScala.toSeq
        tokens.take(40)
        }).cache()

    // Count total lines
    val totalLines = tokenized.count() // Count total number of lines

//Key Transformations:

// flatMap: One-to-many transformation
// distinct: Remove duplicates
// reduceByKey: Aggregate by key
// collectAsMap: Action to create local map

    // Count individual words
    // Aggregation requires shuffle
    // Fix here with the type annotation for wordCounts
    val wordCounts: scala.collection.Map[String, Int] = tokenized
      .flatMap(tokens => tokens.distinct) // Get unique tokens from each line // Local to each worker
      .map(word => (word, 1)) // Create word-count pairs // Local to each worker
      .reduceByKey(_ + _) // Sum counts for each word // Requires network shuffle
      .filter(_._2 >= conf.threshold) // Filter by minimum threshold
      .collectAsMap() // Convert to local map 
    //   graph TD
    //   A[RDD Distributed across Workers] 
    //   --> B[collectAsMap]
    //   --> C[Single Map in Driver Memory]
    //   --> D[Broadcast to All Workers]      

    // Broadcast word counts
     // Broadcast word counts to workers
    val broadcastCounts = sc.broadcast(wordCounts)// Collects to driver node's memory
     // Broadcast word counts to workers
     // Broadcast from driver
     //Driver coordinates distribution to workers
//Single source of truth for broadcast

    // Second pass - generate and count pairs, calculate PMI
    val pmi = tokenized
      .flatMap(tokens => { // Generate word pairsa
        // Fixed tokens reference
        for {
          i <- tokens.indices
          j <- (i + 1) until tokens.length
          if broadcastCounts.value.contains(tokens(i)) &&  // Check both words exist
             broadcastCounts.value.contains(tokens(j))
        } yield ((tokens(i), tokens(j)), 1) // Emit pair with count 1
      })
      .reduceByKey(_ + _) // Sum pair occurrences
      .filter(_._2 >= config.threshold)
      .map { case ((word1, word2), count) =>
        val pxy = count.toDouble / totalLines
        val px = broadcastCounts.value(word1).toDouble / totalLines
        val py = broadcastCounts.value(word2).toDouble / totalLines
        ((word1, word2), (math.log10(pxy / (px * py)), count))
      }
      .coalesce(config.numReducers)

//This is the first of two passes needed for PMI calculation, gathering global statistics (word counts and total lines).

      // Save output
      pmi.saveAsTextFile(config.outputPath)
    } finally {
      sc.stop()
    }
  }
}
