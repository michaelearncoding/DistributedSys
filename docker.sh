# First time

docker build -t hadoop-pseudo-distributed .

docker stop hadoop-pseudo

docker rm hadoop-pseudo

docker run -it --name hadoop-pseudo -v /Users/qingdamai/Desktop/helloHaddop:/mnt/helloHaddop -p 9870:9870 -p 8088:8088 -p 9000:9000 -p 8042:8042 -p 9864:9864 -p 9868:9868 -p 8080:8080 -p 8081:8081 hadoop-pseudo-distributed

# 通过以下命令进入容器：
docker exec -it hadoop-pseudo /bin/bash

cd /mnt/helloHaddop/

# Hadoop Cmd
hadoop jar target/assignments-1.0.jar ca.uwaterloo.cs651.a0.WordCount -input data/Shakespeare.txt -output wc

# Check the output
hdfs dfs -ls /user/hadoop/wc
hdfs dfs -cat /user/hadoop/wc/part-r-00000


# Go to the hadoop resource manager web interface
# Find the application id of the job
yarn application -status application_1738352789409_0001

# Command to find top 10 most frequent words
hdfs dfs -cat /user/hadoop/wc/part-r-00000 | sort -k2 -nr | head -10


# 通过以上步骤，你已经成功运行了Hadoop作业，并验证了输出。以下是一些关键点：

# 修改启动脚本：确保所有Hadoop守护进程在同一个容器中启动，并避免使用SSH。
# 修改Hadoop配置：确保Hadoop配置文件正确配置，以避免使用SSH。
# 上传输入文件到HDFS：确保输入文件存在于HDFS中。
# 运行Hadoop作业：成功运行Hadoop作业，并验证输出。
# 如果你在访问过程中遇到问题，请检查Docker容器的配置和日志，以确定问题的根本原因。



# Rebuild the maven package

# Enter container
docker exec -it hadoop-pseudo bash

# Go to mounted directory
cd /mnt/helloHaddop

# Run Maven
mvn clean package

# put the data file into HDFS
# /mnt/helloHaddop$ hdfs dfs -put data/wiki_sample_processed.txt /user/hadoop/data/


# 1. First verify input file exists in HDFS
hdfs dfs -ls /user/hadoop/data/wiki_sample_processed.txt

# 2. If file doesn't exist, copy it to HDFS
hdfs dfs -put data/wiki_sample_processed.txt /user/hadoop/data/


hadoop jar target/assignments-1.0.jar ca.uwaterloo.cs651.a0.PerfectX data/wiki_sample_processed.txt pxhdfs

hdfs dfs -cat /user/hadoop/px/part-r-00000

