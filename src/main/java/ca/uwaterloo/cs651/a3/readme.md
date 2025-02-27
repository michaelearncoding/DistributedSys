# Plan:


```text
1. Index Compression

Use VInt encoding for document IDs and frequencies

Implement gap compression for doc IDs

Store compressed postings as raw bytes


2. Buffering & Memory Management
Implement spill-to-disk when buffer exceeds threshold

Use k-way merge for combining spilled files

Compress postings before buffering


3. Term Partitioning
Add reducer count parameter

Implement custom partitioner

Ensure consistent partitioning for search


```

## Compression Implementation


### Benefits
Smaller numbers = fewer bytes

Sequential IDs compress well

Significant space savings

Maintains sortability

### This compression helps reduce:

Storage space

Memory usage

Network transfer

### Example: 

Original document IDs: [5, 8, 12, 15]

```text

Original document IDs: [5, 8, 12, 15]

1. VInt Compression:
- Small numbers use fewer bytes
- 5 -> [5]
- 128 -> [128, 1]

2. Gap Compression:

Original: [5, 8, 12, 15]

Gaps:    [5, 3,  4,  3]
         ↑  ↑   ↑   ↑
         5  8-5 12-8 15-12

Final Compressed: [5, 3, 4, 3]

```




```java
private static class IndexCompressor {
    public static void writeVInt(DataOutput out, int value) throws IOException {
        WritableUtils.writeVInt(out, value);
    }
    
    public static void writeGappedDocIds(DataOutput out, List<Integer> docIds) throws IOException {
        int prev = 0;
        for (int docId : docIds) {
            writeVInt(out, docId - prev); // Gap compression
            prev = docId;
        }
    }
}




```





## Term Partitioning

It's a strategy for distributing work across multiple reducers in a MapReduce job when building an inverted index.

### How it works:
Input Distribution
Term = key (e.g., "cat", "dog", "the")

Each term gets assigned to a specific reducer

Based on term's hash value

### Example:

With 3 reducers:
```text
"cat" -> hashCode % 3 = 1 -> Reducer 1

"dog" -> hashCode % 3 = 2 -> Reducer 2

"the" -> hashCode % 3 = 0 -> Reducer 0




输入：
key = "cat"
value = <1,3>  // 文档1中出现3次
numPartitions = 4

处理过程：
1. key.toString() = "cat"
2. "cat".hashCode() = 123456
3. 123456 & Integer.MAX_VALUE = 123456  // 确保正数
4. 123456 % 4 = 0  // 确定发送到哪个reducer

输出：
0  // 表示这个词会被发送到Reducer 0处理


```
### Benefits:
Parallel processing

Better load distribution

Scales with data size

```java
// Set in job configuration
job.setNumReduceTasks(args.numReducers);
job.setPartitionerClass(TermPartitioner.class);

````


### Guarantees:

Same term always goes to same reducer

All postings for a term processed together

Even distribution of work


