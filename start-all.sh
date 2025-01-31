#!/bin/bash

# 格式化 HDFS NameNode（仅在第一次启动时需要）
if [ ! -d "/usr/local/hadoop/data/namenode/current" ]; then
  hdfs namenode -format
fi

# 启动 HDFS
$HADOOP_HOME/bin/hdfs --daemon start namenode
$HADOOP_HOME/bin/hdfs --daemon start datanode
$HADOOP_HOME/bin/hdfs --daemon start secondarynamenode

# 启动 YARN
$HADOOP_HOME/bin/yarn --daemon start resourcemanager
$HADOOP_HOME/bin/yarn --daemon start nodemanager

# 启动 Spark
$SPARK_HOME/sbin/start-master.sh
# $SPARK_HOME/sbin/start-worker.sh spark://localhost:7077 #deprecated
$SPARK_HOME/sbin/start-slave.sh spark://localhost:7077


# 保持容器运行
/bin/bash