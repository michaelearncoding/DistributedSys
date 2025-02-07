package ca.uwaterloo.cs651.a1; // Package declaration

import io.bespin.java.util.Tokenizer;  // Custom tokenizer for text processing
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

// Hadoop filesystem
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

// Hadoop I/O types
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

// MapReduce core classes
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class PairsPMI extends Configured implements Tool {  // Provides standard interface for Hadoop tools
    //Inheritance from base class
    //PairsPMI inherits all non-private methods/fields from Configured class
    // Interface implementation
    // Contract to implement required methods

    private static final Logger LOG = Logger.getLogger(PairsPMI.class);

    // Configured: Base class for Hadoop tools
    // Tool: Interface for running MapReduce jobs

    // First Job: Count occurrences
    public static class LineCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        
        @Override
        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            List<String> tokens = Tokenizer.tokenize(value.toString());
            tokens = tokens.subList(0, Math.min(tokens.size(), 40));
            
            context.write(new Text("*"), ONE);  // Count total lines
            for (String token : new HashSet<>(tokens)) {
                context.write(new Text(token), ONE);
            }
        }
    }

    public static class LineCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private static final IntWritable SUM = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            SUM.set(sum);
            context.write(key, SUM);
        }
    }

    // Second Job: Calculate PMI
    public static class PMIMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private int threshold;

        @Override
        public void setup(Context context) {
            threshold = context.getConfiguration().getInt("threshold", 10);
        }

        @Override
        public void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            List<String> tokens = Tokenizer.tokenize(value.toString());
            tokens = tokens.subList(0, Math.min(tokens.size(), 40));
            
            for (int i = 0; i < tokens.size(); i++) {
                for (int j = i + 1; j < tokens.size(); j++) {
                    context.write(new Text(tokens.get(i) + "," + tokens.get(j)), ONE);
                    context.write(new Text(tokens.get(j) + "," + tokens.get(i)), ONE);
                }
            }
        }
    }

    public static class PMIReducer extends Reducer<Text, IntWritable, Text, DoubleWritable> {
        private long totalLines;
        private Map<String, Integer> wordCounts;
        private int threshold;
        private static final DoubleWritable PMI_VALUE = new DoubleWritable();

        @Override
        public void setup(Context context) throws IOException {
            Configuration conf = context.getConfiguration();
            totalLines = conf.getLong("totalLines", 1);
            threshold = conf.getInt("threshold", 10);
            wordCounts = new HashMap<>();
            
            // Load word counts from first job
            Path[] cacheFiles = context.getLocalCacheFiles();
            if (cacheFiles != null && cacheFiles.length > 0) {
                loadWordCounts(cacheFiles[0]);
            }
        }

        private void loadWordCounts(Path path) throws IOException {
            try (Scanner scan = new Scanner(new File(path.toString()))) {
                while (scan.hasNextLine()) {
                    String[] parts = scan.nextLine().split("\t");
                    if (parts.length == 2) {
                        wordCounts.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }
        }

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int pairCount = 0;
            for (IntWritable val : values) {
                pairCount += val.get();
            }

            if (pairCount >= threshold) {
                String[] words = key.toString().split(",");
                if (words.length == 2 && wordCounts.containsKey(words[0]) && 
                    wordCounts.containsKey(words[1])) {
                    double pmi = calculatePMI(words[0], words[1], pairCount);
                    PMI_VALUE.set(pmi);
                    context.write(key, PMI_VALUE);
                }
            }
        }

        private double calculatePMI(String word1, String word2, int pairCount) {
            double p_xy = (double) pairCount / totalLines;
            double p_x = (double) wordCounts.get(word1) / totalLines;
            double p_y = (double) wordCounts.get(word2) / totalLines;
            return Math.log10(p_xy / (p_x * p_y));
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        
        // First job: Count words and lines
        Job job1 = Job.getInstance(conf, "PMI-Count");
        job1.setJarByClass(PairsPMI.class);
        
        job1.setMapperClass(LineCountMapper.class);
        job1.setCombinerClass(LineCountReducer.class);
        job1.setReducerClass(LineCountReducer.class);
        
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job1, new Path(args[0]));
        FileOutputFormat.setOutputPath(job1, new Path(args[1] + "_counts"));
        
        job1.waitForCompletion(true);

        // Second job: Calculate PMI
        Job job2 = Job.getInstance(conf, "PMI-Calculate");
        job2.setJarByClass(PairsPMI.class);
        
        job2.setMapperClass(PMIMapper.class);
        job2.setReducerClass(PMIReducer.class);
        
        job2.setOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(IntWritable.class);
        job2.setOutputValueClass(DoubleWritable.class);
        
        FileInputFormat.addInputPath(job2, new Path(args[0]));
        FileOutputFormat.setOutputPath(job2, new Path(args[1]));
        
        // Add word counts to distributed cache
        job2.addCacheFile(new Path(args[1] + "_counts/part-r-00000").toUri());
        
        return job2.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new PairsPMI(), args);
        System.exit(res);
    }
}