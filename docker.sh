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


# Yarn resource manager cmd

# Verify services
jps | grep -E "ResourceManager|NodeManager"

# 1. Kill hanging applications
yarn application -kill application_1739310151077_0001
yarn application -kill application_1739310151077_0002


# Kill unnecessary Spark applications
for pid in 1994 1450 1618 4309 3228; do
  kill -9 $pid
done

# Verify core services
required_services="NameNode DataNode SecondaryNameNode NodeManager Master Worker"
jps | grep -E "$(echo $required_services | tr ' ' '|')"

# Restart YARN NodeManager if needed
$HADOOP_HOME/sbin/yarn-daemon.sh stop nodemanager
$HADOOP_HOME/sbin/yarn-daemon.sh start nodemanager



# The warning shows we're using a deprecated script. Let's use the newer commands.
yarn --daemon stop resourcemanager
yarn --daemon stop nodemanager

# 2. Start with new commands
yarn --daemon start resourcemanager
yarn --daemon start nodemanager

# 3. Clean up any stale PID files
rm -f $HADOOP_HOME/logs/*.pid

# 1. Kill ApplicationCLI process
kill -9 4659


# 2. Check for ResourceManager/NodeManager processes not shown in ps
kill -9 $(pgrep -f "resourcemanager|nodemanager")

# 3. Clean up logs and PID files
rm -f ${HADOOP_HOME}/logs/yarn-*-*.pid
rm -f ${HADOOP_HOME}/logs/yarn-*-*.out




# 1. Kill any existing YARN processes
pkill -f resourcemanager
pkill -f nodemanager



##### Final fix for the resource manager error: 

# 1. Create capacity-scheduler.xml
cat > $HADOOP_HOME/etc/hadoop/capacity-scheduler.xml << EOF
<configuration>
  <property>
    <name>yarn.scheduler.capacity.root.queues</name>
    <value>default</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.default.capacity</name>
    <value>100</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.default.state</name>
    <value>RUNNING</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.root.default.minimum-user-limit-percent</name>
    <value>100</value>
  </property>
  <property>
    <name>yarn.scheduler.capacity.resource-calculator</name>
    <value>org.apache.hadoop.yarn.util.resource.DominantResourceCalculator</value>
  </property>
</configuration>
EOF

# 2. Start ResourceManager
$HADOOP_HOME/bin/yarn resourcemanager

# 1. Check ResourceManager status
jps | grep ResourceManager

# 2. Check node status
yarn node -list

# 3. Submit Spark job with working ResourceManager
spark-submit \
  --class ca.uwaterloo.cs651.a2.PairsPMI \
  --master yarn \
  --deploy-mode client \
  --driver-memory 512m \
  --executor-memory 1g \
  --executor-cores 1 \
  --num-executors 1 \
  --conf spark.yarn.am.memory=512m \
  /mnt/helloHaddop/target/assignments-1.0.jar \
  --input /user/hadoop/data/Shakespeare.txt \
  --output /user/hadoop/output/shakespeare-pmi-scala \
  --reducers 1 \
  --threshold 10



# 3. Restart YARN services properly
$HADOOP_HOME/bin/yarn --daemon stop resourcemanager
$HADOOP_HOME/bin/yarn --daemon stop nodemanager
$HADOOP_HOME/bin/yarn --daemon start resourcemanager
$HADOOP_HOME/bin/yarn --daemon start nodemanager


# Submit Spark job with adjusted resources
cd /mnt/helloHaddop

spark-submit \
  --class ca.uwaterloo.cs651.a2.PairsPMI \
  --master yarn \
  --deploy-mode client \
  --driver-memory 512m \
  --executor-memory 1g \
  --executor-cores 1 \
  --num-executors 1 \
  --conf spark.yarn.am.memory=896m \
  target/assignments-1.0.jar \
  --input /user/hadoop/data/Shakespeare.txt \
  --output /user/hadoop/output/shakespeare-pmi-scala \
  --reducers 1 \
  --threshold 10

# Check output
hdfs dfs -ls /user/hadoop/output/shakespeare-pmi-scala
hdfs dfs -cat /user/hadoop/output/shakespeare-pmi-scala/part-* | head -n 10


