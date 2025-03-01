# 1. 基本读取
# 基本读取JSON文件
df = spark.read.json("/path/to/json/files/*.json")

# 或指定schema提高性能
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, ArrayType
schema = StructType([
  StructField("user_id", IntegerType()),
  StructField("name", StringType()),
  StructField("timestamp", StringType()),
  # ...其他字段
])
df = spark.read.schema(schema).json("/path/to/files")




# 2. 时间戳处理
from pyspark.sql.functions import col, to_timestamp

# 假设JSON中有timestamp字段
df = df.withColumn("processed_time", to_timestamp(col("timestamp")))

# 或从文件名提取时间戳
from pyspark.sql.functions import input_file_name, regexp_extract
df = df.withColumn("file_path", input_file_name())
df = df.withColumn("file_date", regexp_extract("file_path", "data_(\\d{8})", 1))



# 3. 增量处理 (Autoloader)
# 自动加载新到达的文件
df = spark.readStream.format("cloudFiles")
  .option("cloudFiles.format", "json")
  .option("cloudFiles.schemaLocation", "/dbfs/path/to/schema")
  .load("/path/to/json/files")



# 4. Delta Live Tables
import dlt
from pyspark.sql.functions import *

@dlt.table
def processed_user_data():
  return (
    spark.read.json("/path/to/json")
    .withColumn("processed_time", current_timestamp())
    .dropDuplicates(["user_id"])
  )



# 这两个工具通常组合使用：Autoloader处理原始数据摄取，DLT构建可靠的处理管道，最终结果存储为Delta表格式。


# 如何连接它们
# 要实现您描述的自动处理流程，需要在DLT中使用Autoloader功能：


@dlt.table
def processed_user_data():
  return (
    # 使用dlt.read_stream代替spark.read来实现自动加载
    dlt.read_stream("cloudFiles")
    .option("cloudFiles.format", "json") 
    .option("cloudFiles.schemaLocation", "/dbfs/path/to/schema")
    .load("/path/to/json/files")
    .withColumn("processed_time", current_timestamp())
    .dropDuplicates(["user_id"])
  )

# 完整流程
# 使用这种连接方式：

# Autoloader持续监控指定位置的新文件
# 当新文件出现时，会自动触发处理
# DLT将处理结果存为Delta表
# 整个过程无需手动干预
# 这就实现了端到端的自动化数据管道，从文件入湖到提供可查询的Delta表。



# Autoloader 与 Delta Live Tables 之间的关系
# 这两段代码在当前形式下不是自动连接的。它们展示了两种不同的功能：

# 关键区别
# Autoloader示例：设置了一个流式处理，持续监控新文件
# DLT示例：使用了spark.read.json()，这是批处理模式，非流式
# 如何连接它们
# 要实现您描述的自动处理流程，需要在DLT中使用Autoloader功能：

