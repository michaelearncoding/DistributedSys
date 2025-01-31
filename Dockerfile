# 使用Ubuntu基础镜像
FROM ubuntu:18.04

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    wget \
    curl \
    vim \
    maven \
    openssh-client

# 创建Hadoop用户和组
RUN groupadd hadoop && useradd -g hadoop hadoop


# 安装Scala 2.11.8
RUN wget https://downloads.lightbend.com/scala/2.11.8/scala-2.11.8.deb && \
    dpkg -i scala-2.11.8.deb && \
    rm scala-2.11.8.deb

# 下载并安装Spark 2.3.1
RUN wget https://archive.apache.org/dist/spark/spark-2.3.1/spark-2.3.1-bin-hadoop2.7.tgz && \
    tar -xzvf spark-2.3.1-bin-hadoop2.7.tgz && \
    mv spark-2.3.1-bin-hadoop2.7 /usr/local/spark && \
    rm spark-2.3.1-bin-hadoop2.7.tgz

# 设置Spark环境变量
ENV SPARK_HOME=/usr/local/spark
ENV PATH=$SPARK_HOME/bin:$PATH

# 设置Java环境变量
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64
ENV PATH=$JAVA_HOME/bin:$PATH

# 创建Maven本地仓库目录并设置权限
RUN mkdir -p /home/hadoop/.m2/repository && chown -R hadoop:hadoop /home/hadoop/.m2

# 下载并安装Hadoop 3.0.3
RUN wget https://archive.apache.org/dist/hadoop/common/hadoop-3.0.3/hadoop-3.0.3.tar.gz && \
    tar -xzvf hadoop-3.0.3.tar.gz && \
    mv hadoop-3.0.3 /usr/local/hadoop && \
    rm hadoop-3.0.3.tar.gz

# 设置Hadoop环境变量
ENV HADOOP_HOME=/usr/local/hadoop
ENV PATH=$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH

# 配置Hadoop
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64" >> $HADOOP_HOME/etc/hadoop/hadoop-env.sh
RUN echo "export HADOOP_SSH_OPTS=\"-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null\"" >> $HADOOP_HOME/etc/hadoop/hadoop-env.sh
RUN echo '<configuration><property><name>fs.defaultFS</name><value>hdfs://localhost:9000</value></property></configuration>' > $HADOOP_HOME/etc/hadoop/core-site.xml
RUN echo '<configuration><property><name>dfs.replication</name><value>1</value></property><property><name>dfs.namenode.name.dir</name><value>file:///usr/local/hadoop/data/namenode</value></property><property><name>dfs.datanode.data.dir</name><value>file:///usr/local/hadoop/data/datanode</value></property></configuration>' > $HADOOP_HOME/etc/hadoop/hdfs-site.xml
RUN echo '<configuration><property><name>mapreduce.framework.name</name><value>yarn</value></property></configuration>' > $HADOOP_HOME/etc/hadoop/mapred-site.xml
RUN echo '<configuration><property><name>yarn.nodemanager.aux-services</name><value>mapreduce_shuffle</value></property></configuration>' > $HADOOP_HOME/etc/hadoop/yarn-site.xml
RUN echo "localhost" > $HADOOP_HOME/etc/hadoop/slaves

# 设置HDFS和YARN用户环境变量
ENV HDFS_NAMENODE_USER=hadoop
ENV HDFS_DATANODE_USER=hadoop
ENV HDFS_SECONDARYNAMENODE_USER=hadoop
ENV YARN_RESOURCEMANAGER_USER=hadoop
ENV YARN_NODEMANAGER_USER=hadoop

# 创建必要的目录并设置权限
RUN mkdir -p /usr/local/hadoop/logs /usr/local/hadoop/data/namenode /usr/local/hadoop/data/datanode
RUN chown -R hadoop:hadoop /usr/local/hadoop
RUN chown -R hadoop:hadoop $SPARK_HOME

# 复制启动脚本并设置权限
COPY start-all.sh /usr/local/bin/start-all.sh
RUN chmod +x /usr/local/bin/start-all.sh


# 配置Spark
RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64" >> $SPARK_HOME/conf/spark-env.sh
RUN echo "export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop" >> $SPARK_HOME/conf/spark-env.sh
RUN echo "export SPARK_MASTER_HOST=localhost" >> $SPARK_HOME/conf/spark-env.sh


# 切换到Hadoop用户
USER hadoop

# 暴露必要的端口
EXPOSE 9870 8088 9000 8042

# 启动Hadoop
CMD ["/usr/local/bin/start-all.sh"]



