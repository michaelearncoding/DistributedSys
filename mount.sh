docker start hadoop-pseudo
docker exec -it hadoop-pseudo /bin/bash
cd /mnt/helloHaddop/

cd /mnt/helloHaddop/src/main/java/ca/uwaterloo/cs651/a0

cd /mnt/helloHaddop/

mvn clean package


project-root/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ca/
│   │   │       └── uwaterloo/
│   │   │           └── cs651/
│   │   │               └── a0/
│   │   │                   └── WordCount.java
│   └── test/
│       ├── java/
│       ├── resources/
└── target/
└── assignments-1.0.jar



project-root/
├── pom.xml                             # Maven 配置文件(必需)
├── src/
│   ├── main/                           # 主代码目录
│   │   ├── java/                       # Java 源代码
│   │   │   └── [包结构目录]           # 按包名组织的源代码
│   │   └── resources/                  # 资源文件目录(可选)
│   └── test/                           # 测试代码目录
│       ├── java/                       # 测试源代码
│       └── resources/                  # 测试资源文件(可选)
└── target/                             # 编译输出目录(自动生成)