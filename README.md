# DistributedSys
Save the source code for the local Distributed System


Download the source data here: 
https://www.kaggle.com/datasets/ltcmdrdata/plain-text-wikipedia-202011?resource=download

[1]kaggle: Plain Text Wikipedia 2020-11
 
[2]Wikipedia text download: 
https://stackoverflow.com/questions/2683506/wikipedia-text-download

[3] a0:
https://student.cs.uwaterloo.ca/~cs451/assignment0-451.html 

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


project-root/
├── pom.xml                             # Maven configuration file (required)
├── src/
│   ├── main/                           # Main source directory
│   │   ├── java/                       # Java source code
│   │   │   └── [package structure]     # Source code organized by package name
│   │   └── resources/                  # Resource files directory (optional)
│   └── test/                           # Test source directory
│       ├── java/                       # Test source code
│       └── resources/                  # Test resource files (optional)
└── target/                             # Compiled output directory (auto-generated)



map reduce：

输入文本:
"This is perfect weather. The perfect timing."

Map阶段输出:
<"weather", 1>
<"timing", 1>

Shuffle & Sort阶段:
按key分组
"weather" -> [1]
"timing" -> [1]

Reduce阶段:
如果计数>1才输出

