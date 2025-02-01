package ca.uwaterloo.cs651.a0;

import io.bespin.java.util.Tokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
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
import java.util.List;

public class PerfectX extends Configured implements Tool {
    private static final Logger LOG = Logger.getLogger(PerfectX.class);

    public static final class XMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private static final Text WORD = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            List<String> wordList = Tokenizer.tokenize(value.toString());
            String[] words = wordList.toArray(new String[0]);
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equalsIgnoreCase("perfect")) {
                    WORD.set(words[i + 1]);
                    context.write(WORD, ONE);
                }
            }
        }
    }

    public static final class XReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private static final IntWritable SUM = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            if (sum > 1) {
                SUM.set(sum);
                context.write(key, SUM);
            }
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        Job job = Job.getInstance(conf, "PerfectX");
        job.setJarByClass(PerfectX.class);
        job.setMapperClass(XMapper.class);
        job.setReducerClass(XReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        Path outputDir = new Path(args[1]);
        FileSystem.get(conf).delete(outputDir, true);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new PerfectX(), args);
        System.exit(res);
    }
}