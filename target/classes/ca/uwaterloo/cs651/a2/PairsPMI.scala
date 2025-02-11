package ca.uwaterloo.cs651.a2; // Package declaration

import org.apache.spark.SparkContext  // Core Spark functionality
import org.apache.spark.SparkConf // Spark configuration
import org.rogach.scallop._ // Command line argument parsing
import scala.collection.mutable.HashMap // Mutable hash map collection

// Main required options
class Conf(args: Seq[String]) extends ScallopConf(args) {
// Define command line arguments configuration
  mainOptions = Seq(input, output, reducers) // Main required options
  val input = opt[String](required = true) // Input path parameter
  val output = opt[String](required = true)// Output path parameter
  val reducers = opt[Int](required = true)  // Number of reducer
  val threshold = opt[Int](default = Some(10)) // Minimum occurrence threshold
  verify() // Verify configuration
}

object PairsPMI { // Singleton object
  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)  // Parse command line arguments
    val sc = new SparkContext(new SparkConf().setAppName("PairsPMI"))  // Create Spark context

    // First pass - count lines and words
    // First Pass - Data Processing
    val lines = sc.textFile(conf.input())  // Read input file
    //// RDD partitioned by default

// Each worker processes its partition
    val tokenized = lines.map(line => {  // Transform each line into tokens
      val tokens = Tokenizer.tokenize(line) 
      tokens.take(40) // Take first 40 tokens only
    })

    // Count total lines
    val totalLines = tokenized.count() // Count total number of lines

//Key Transformations:

// flatMap: One-to-many transformation
// distinct: Remove duplicates
// reduceByKey: Aggregate by key
// collectAsMap: Action to create local map

    // Count individual words
    // Aggregation requires shuffle
    val wordCounts = tokenized
      .flatMap(tokens => tokens.distinct) // Get unique tokens from each line // Local to each worker
      .map(word => (word, 1)) // Create word-count pairs // Local to each worker
      .reduceByKey(_ + _) // Sum counts for each word // Requires network shuffle
      .filter(_._2 >= conf.threshold()) // Filter by minimum threshold
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
      .flatMap(e => { // Generate word pairs
        for {
          i <- tokens.indices
          j <- (i + 1) until tokens.length
          if broadcastCounts.value.contains(tokens(i)) &&  // Check both words exist
             broadcastCounts.value.contains(tokens(j))
        } yield ((tokens(i), tokens(j)), 1) // Emit pair with count 1
      })
      .reduceByKey(_ + _) // Sum pair occurrences
      .filter(_._2 >= conf.threshold())  // Filter by threshold
      .map { case ((word1, word2), count) =>
        val pxy = count.toDouble / totalLines
        val px = broadcastCounts.value(word1).toDouble / totalLines // P(x)
        val py = broadcastCounts.value(word2).toDouble / totalLines // P(y)
        ((word1, word2), math.log10(pxy / (px * py))) // PMI formula
      }

//This is the first of two passes needed for PMI calculation, gathering global statistics (word counts and total lines).

    // Save results
    pmi.saveAsTextFile(conf.output())
    sc.stop()
  }
}
