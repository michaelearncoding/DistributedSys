# Distributed Systems Project

This repository contains the source code for a distributed system project focusing on MapReduce implementations.

## Data Sources
The data used in this project can be downloaded from:
- [Kaggle: Plain Text Wikipedia 2020-11](https://www.kaggle.com/ltcmdrdata/plain-text-wikipedia-202011)
- Additional information about Wikipedia text download can be found on Stack Overflow.


## MapReduce Example

**Input Text:**  
" This is perfect weather. The perfect timing."

**Map Phase Output:**  
```text
<"weather", 1>
<"timing", 1>

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


