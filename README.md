# Distributed Systems Project

This repository contains the source code for a distributed system project focusing on MapReduce implementations.

## Data Sources
The data used in this project can be downloaded from:
- [Kaggle: Plain Text Wikipedia 2020-11](https://www.kaggle.com/ltcmdrdata/plain-text-wikipedia-202011)
- Additional information about Wikipedia text download can be found on Stack Overflow.

use Dockerfile & start-all.sh to build a docker image and run a container

make sure the port & necessary local file path is created, so that the code file can be created by the local IDE (persional dev environment)

mkdir -p src/main/java/ca/uwaterloo/cs451/a0

use the cmd "maven clean pacakge " to build the jar, which will be needed when calling hadoop
(More Java Library API docs...)

use the pom.xml directly, 




## Project Structure
```text
project-root/
├── pom.xml           # Maven configuration file (required)
├── src/
│   ├── main/
│   │   ├── java/     # Java source code organized by package structure
│   │   └── resources/ # Resource files directory (optional)
│   └── test/
│       ├── java/     # Test source code
│       └── resources/# Test resource files (optional)
└── target/           # Compiled output directory (auto-generated)
```

## Environment Setup

```text
# First time

docker build -t hadoop-pseudo-distributed .

docker stop hadoop-pseudo

docker rm hadoop-pseudo

docker run -it --name hadoop-pseudo -v /Users/MakeUpusername/Desktop/helloHaddop:/mnt/helloHaddop -p 9870:9870 -p 8088:8088 -p 9000:9000 -p 8042:8042 -p 9864:9864 -p 9868:9868 -p 8080:8080 -p 8081:8081 hadoop-pseudo-distributed

# enter the container through local terminal：
docker exec -it hadoop-pseudo /bin/bash

cd /mnt/helloHaddop/

# Hadoop Cmd
# Have to use the Maven clean package! to generate the jar
hadoop jar target/assignments-1.0.jar ca.uwaterloo.cs651.a0.WordCount -input data/Shakespeare.txt -output wc
```


## MapReduce Example

**Input Text:**  
" This is perfect weather. The perfect timing."

**Map Phase Output:**  
```text
<"weather", 1>
<"timing", 1>
```
Shuffle & Sort Phase:

Group by keys:
```text
"weather" -> [1]
"timing" -> [1]
```

Reduce Phase:

Only output if count > 1.


