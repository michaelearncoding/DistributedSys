/**
 * Bespin: reference implementations of "big data" algorithms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//  package io.bespin.java.mapreduce.search;
 package ca.uwaterloo.cs651.a3; // Package declaration
 import org.apache.hadoop.mapreduce.Partitioner;
 import io.bespin.java.util.Tokenizer;
 import org.apache.hadoop.conf.Configured;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.IntWritable;
 import org.apache.hadoop.io.LongWritable;
 import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.Mapper;
 import org.apache.hadoop.mapreduce.Reducer;
 import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
 import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
 import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
 import org.apache.hadoop.util.Tool;
 import org.apache.hadoop.util.ToolRunner;
 import org.apache.log4j.Logger;
 import org.kohsuke.args4j.CmdLineException;
 import org.kohsuke.args4j.CmdLineParser;
 import org.kohsuke.args4j.Option;
 import org.kohsuke.args4j.ParserProperties;
 import tl.lin.data.array.ArrayListWritable;
 import tl.lin.data.fd.Object2IntFrequencyDistribution;
 import tl.lin.data.fd.Object2IntFrequencyDistributionEntry;
 import tl.lin.data.pair.PairOfInts;
 import tl.lin.data.pair.PairOfObjectInt;
 import tl.lin.data.pair.PairOfWritables;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.PriorityQueue;
import java.io.File;

import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;



 
 // 1. 类的定义
 public class BuildInvertedIndex extends Configured implements Tool {
  // 用于记录日志的对象
   private static final Logger LOG = Logger.getLogger(BuildInvertedIndex.class);
   // 1. 工具类
// private static final Logger LOG = Logger.getLogger();


// 2. 计数器/统计
// private static int totalUsers = 0;

    // // 静态成员：可以直接通过类访问
    // Math.PI              // 不需要创建实例
    // Logger.getLogger()   // 静态方法调用

    // 非静态成员：必须通过实例访问
    // Counter c = new Counter();
    // c.instanceCount      // 需要实例
 
// 3. 共享资源
// private static final DatabaseConnection DB;

// 4. 常量
// public static final double PI = 3.14159;

// 2. Mapper类 - 处理输入文档并生成词-文档对
   private static final class MyMapper extends Mapper<LongWritable, Text, Text, PairOfInts> {
     // WORD用于存储分词结果
     private static final Text WORD = new Text();
     // COUNTS用于统计每个词在文档中出现的次数
     private static final Object2IntFrequencyDistribution<String> COUNTS =
         new Object2IntFrequencyDistributionEntry<>();

//Mapper的核心工作流程:

// 接收文档内容
// 对文档进行分词
// 统计每个词的出现次数
// 输出 <词, (文档ID,出现次数)> 的键值对         

     @Override
     public void map(LongWritable docno, Text doc, Context context)
         throws IOException, InterruptedException {
       List<String> tokens = Tokenizer.tokenize(doc.toString());
 
       // Build a histogram of the terms.
       COUNTS.clear();
       for (String token : tokens) {
         COUNTS.increment(token);
       }
 
       // Emit postings.
       for (PairOfObjectInt<String> e : COUNTS) {
         WORD.set(e.getLeftElement());
         context.write(WORD, new PairOfInts((int) docno.get(), e.getRightElement()));
       }
     }
   }

// 数据结构的类别

/*
书 = 文档集合
单词 = 索引项
页码 = 文档ID
出现次数 = 该单词在文档中出现多少次


输入：
文档1: "the cat sat"
文档2: "the cat ran"
文档3: "the dog ran"

*/ 


//Reducer的核心工作流程:

// 接收相同词的所有文档信息
// 将文档信息整理成列表
// 计算在多少文档中出现(df值)
// 输出 <词, (文档频率,文档列表)>

   // 3. Reducer类 - 合并相同词的文档列表

   /*
   输入到reduce函数:
key = "cat"
values = [(文档1,1), (文档2,1)]

处理过程:
- 创建空列表 postings = []
- 遍历values, 添加到postings
- 计算df (df=2, 因为"cat"出现在2个文档中)
- 排序postings
- 输出结果

最终输出形式：


cat -> (df=2, [(文档1,1), (文档2,1)])
dog -> (df=1, [(文档3,1)])
the -> (df=3, [(文档1,1), (文档2,1), (文档3,1)])

核心思想就是：

把相同单词在不同文档中的出现情况整理在一起
记录这个单词总共出现在多少个文档中(df值)
把所有信息按文档ID排序后存储
   */

   // Modified Reducer with buffering
   private static final class MyReducer extends
       Reducer<Text, PairOfInts, Text, PairOfWritables<IntWritable, ArrayListWritable<PairOfInts>>> {
        private static final int BUFFER_SIZE = 100000; // Adjust based on memory constraints
        private List<PairOfInts> buffer;
        private File tempFile;
        private int spillCount = 0;
        private Context context; // Add this field
//here
        // Add inside MyReducer class:

        private void mergeSpills() throws IOException {
          List<ObjectInputStream> streams = new ArrayList<>();
          PriorityQueue<MergeEntry> queue = new PriorityQueue<>((a, b) -> a.current.compareTo(b.current));
          
          try {
              // Open all spill files and initialize queue
              for (int i = 0; i < spillCount; i++) {
                  File spillFile = new File("spill_" + context.getTaskAttemptID() + "_" + i);
                  ObjectInputStream in = new ObjectInputStream(new FileInputStream(spillFile));
                  streams.add(in);
                  
                  @SuppressWarnings("unchecked")
                  List<PairOfInts> spilledData = (List<PairOfInts>) in.readObject();
                  if (!spilledData.isEmpty()) {
                      Iterator<PairOfInts> iterator = spilledData.iterator();
                      queue.add(new MergeEntry(i, iterator.next(), iterator));
                  }
              }
              
              // Perform k-way merge
              buffer.clear();
              while (!queue.isEmpty()) {
                  MergeEntry entry = queue.poll();
                  buffer.add(entry.current);
                  
                  if (entry.iterator.hasNext()) {
                      entry.current = entry.iterator.next();
                      queue.add(entry);
                  }
              }
              
          } catch (ClassNotFoundException e) {
              throw new IOException("Error reading spill files", e);
          } finally {
              // Close all streams
              for (ObjectInputStream stream : streams) {
                  try {
                      stream.close();
                  } catch (IOException e) {
                      // Log error but continue closing others
                      LOG.error("Error closing spill file", e);
                  }
              }
              
              // Delete spill files
              for (int i = 0; i < spillCount; i++) {
                  new File("spill_" + context.getTaskAttemptID() + "_" + i).delete();
              }
              
              spillCount = 0;
          }
        }

        // Helper class for merge process
        private static class MergeEntry {
          PairOfInts current;
          final Iterator<PairOfInts> iterator;
          
          MergeEntry(int index, PairOfInts current, Iterator<PairOfInts> iterator) {
              this.current = current;
              this.iterator = iterator;
          }
        }
        



        @Override
        protected void setup(Context context) {
            this.context = context; // Store context
            buffer = new ArrayList<>();
            tempFile = new File("spill_" + context.getTaskAttemptID());
        }

        private void spillBuffer() throws IOException {
            Collections.sort(buffer);
            // Write buffer to temp file
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                out.writeObject(buffer);
            }
            buffer.clear();
            spillCount++;
        }
        
        @Override
        public void reduce(Text key, Iterable<PairOfInts> values, Context context) 
                throws IOException, InterruptedException {
            for (PairOfInts value : values) {
                buffer.add(value.clone());
                if (buffer.size() >= BUFFER_SIZE) {
                    spillBuffer();
                }
            }
            
            // Merge all spilled runs
            if (spillCount > 0) {
                mergeSpills();
            }
            
            // Compress and write final output
            writeCompressedPostings(key, buffer, context);
        }
        

        private void writeCompressedPostings(Text key, List<PairOfInts> postings, Context context) 
                throws IOException, InterruptedException {
            // Sort postings by document ID
            Collections.sort(postings);
            
            // Create compressed output
            ArrayListWritable<PairOfInts> compressed = new ArrayListWritable<>();
            
            // Extract doc IDs and frequencies
            List<Integer> docIds = new ArrayList<>();
            List<Integer> frequencies = new ArrayList<>();
            for (PairOfInts posting : postings) {
                docIds.add(posting.getLeftElement());
                frequencies.add(posting.getRightElement());
            }
            
            // Use IndexCompressor to compress doc IDs with gap encoding
            DataOutput tempOutput = new DataOutputStream(new ByteArrayOutputStream());
            IndexCompressor.writeGappedDocIds(tempOutput, docIds);
            
            // Write frequencies using VInt compression
            for (int freq : frequencies) {
                IndexCompressor.writeVInt(tempOutput, freq);
            }
            
            // Write final output
            context.write(key, new PairOfWritables<>(
                new IntWritable(postings.size()),
                compressed
            ));
        }



    }
    
    // Custom Partitioner for term distribution
    public static class TermPartitioner extends Partitioner<Text, PairOfInts> {
        @Override
        public int getPartition(Text key, PairOfInts value, int numPartitions) {
            return (key.toString().hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }
 

 
   private BuildInvertedIndex() {}

    // Helper class for compression
  private static class IndexCompressor {
        public static void writeVInt(DataOutput out, int value) throws IOException {
            WritableUtils.writeVInt(out, value);
        }
        
        public static void writeGappedDocIds(DataOutput out, List<Integer> docIds) throws IOException {
            int prev = 0;
            for (int docId : docIds) {
                writeVInt(out, docId - prev); // Store gaps instead of absolute values
                prev = docId;
            }
        }
    }       

   private static final class Args {
    // @Option 是一个注解，用于标记命令行参数的配置：
    @Option(name = "-numReducers", metaVar = "[num]", required = false, usage = "number of reducers")
    int numReducers = 1;

     @Option(name = "-input", metaVar = "[path]", required = true, usage = "input path")
     String input;
 
     @Option(name = "-output", metaVar = "[path]", required = true, usage = "output path")
     String output;
   }
 
    // Custom Partitioner for term distribution

   /**
    * Runs this tool.
    */
   @Override
   public int run(String[] argv) throws Exception {
     final Args args = new Args();
     CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(100));
 
     try {
       parser.parseArgument(argv);
     } catch (CmdLineException e) {
       System.err.println(e.getMessage());
       parser.printUsage(System.err);
       return -1;
     }
 
     LOG.info("Tool: " + BuildInvertedIndex.class.getSimpleName());
     LOG.info(" - input path: " + args.input);
     LOG.info(" - output path: " + args.output);
 
     Job job = Job.getInstance(getConf());
     job.setJobName(BuildInvertedIndex.class.getSimpleName());
     job.setJarByClass(BuildInvertedIndex.class);
 
     job.setNumReduceTasks(1);
 
     FileInputFormat.setInputPaths(job, new Path(args.input));
     FileOutputFormat.setOutputPath(job, new Path(args.output));
 
     job.setMapOutputKeyClass(Text.class);
     job.setMapOutputValueClass(PairOfInts.class);
     job.setOutputKeyClass(Text.class);
     job.setOutputValueClass(PairOfWritables.class);
     job.setOutputFormatClass(MapFileOutputFormat.class);
 
     job.setMapperClass(MyMapper.class);
     job.setReducerClass(MyReducer.class);

    // Set number of reducers from args
    job.setNumReduceTasks(args.numReducers);
    job.setPartitionerClass(TermPartitioner.class);    
 
     // Delete the output directory if it exists already.
     Path outputDir = new Path(args.output);
     FileSystem.get(getConf()).delete(outputDir, true);
 
     long startTime = System.currentTimeMillis();
     job.waitForCompletion(true);
     System.out.println("Job Finished in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
 
     return 0;
   }
 
   /**
    * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
    *
    * @param args command-line arguments
    * @throws Exception if tool encounters an exception
    */
  
    // 4. 程序入口
   public static void main(String[] args) throws Exception {
     // 启动MapReduce作业
     ToolRunner.run(new BuildInvertedIndex(), args);
   }
 }
 