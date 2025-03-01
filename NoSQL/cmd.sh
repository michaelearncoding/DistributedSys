# Redis（Key-Value Store）
# Redis 适用于 缓存、会话存储、排行榜、计数器 等场景。
# Redis 适合存储 频繁访问的数据，但不是理想的持久存储。


# 练习场景：用户会话管理
# 你需要存储用户的 登录状态 和 访问令牌，并设置 过期时间（TTL）。
# Redis 适合存储 频繁访问的数据，但不是理想的持久存储。

docker exec -it redis_container redis-cli

# 当用户成功登录时，应用创建会话数据
# 存储用户身份信息和唯一访问令牌
# 应用将令牌返回给客户端(通常作为cookie或JWT)
SET user:1001 '{"username": "ananya", "status": "active", "token": "abc123"}'
# 设置会话过期时间 (EXPIRE 命令),设置60秒后自动过期，增强安全性
# 防止无效会话长期存在,实际应用中可能设置30分钟或更长
EXPIRE user:1001 60
GET user:1001 # 用户每次请求受保护资源时,应用从请求中提取令牌，使用它查找会话,验证会话存在且有效
DEL user:1001 # 用户点击"退出登录"时，立即删除会话,使令牌立即失效，增强安全性


```
应用服务访问Redis: 在这种会话管理架构中，应用服务（后端服务器）访问Redis，而不是客户端应用：

Redis在这个过程中只是一个存储介质，它存储由应用服务生成的token和会话数据，但不参与token的生成过程。

┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│             │  HTTP   │             │  Redis  │             │
│  用户客户端  │◄────────►│  应用服务   │◄────────►│  Redis服务  │
│ (浏览器/App) │  请求   │  (后端)     │  命令   │             │
└─────────────┘         └─────────────┘         └─────────────┘
       ▲                       │
       └───────────────────────┘
           返回token给客户端
```

典型流程
用户登录:

用户通过浏览器/App输入凭据
应用服务验证凭据
应用服务生成会话数据，通过Redis命令存入Redis
应用服务将token返回给客户端
后续请求:

客户端发送请求时附带token
应用服务从Redis获取会话数据验证token
根据验证结果决定是否处理请求
Redis作为一个独立服务，只与应用服务通信，不会直接与客户端通信，这是出于安全和架构设计考虑。

Redis缓存优势在身份验证流程中
Redis的缓存优势 - 会话管理模式
首次登录 → 查询用户数据库 → 验证用户 → 存入Redis → 返回token
后续请求 → 查询Redis → 验证token → 处理请求

优势： 
只在首次登录时查询主数据库
后续请求只查询高速Redis缓存
验证速度极快(微秒级)
减轻主数据库压力
身份验证更高效

Token生成流程
在首次登录过程中，token是由应用服务（后端）生成的，不是由Redis生成的。

详细流程
用户向应用服务器提交用户名和密码
应用服务验证凭据（通常与用户数据库比对）
应用服务生成唯一token
可能使用UUID
或生成JWT (JSON Web Token)
或其他安全随机生成方法
应用服务将用户信息和token存入Redis
应用服务将token返回给客户端
Redis在这个过程中只是一个存储介质，它存储由应用服务生成的token和会话数据，但不参与token的生成过程。

应用服务负责所有的业务逻辑，包括身份验证和token生成，而Redis只负责高性能存储和检索。







# MongoDB（Document Store）
MongoDB 适合存储 灵活的 JSON 结构，可以轻松扩展字段。
# MongoDB 适用于 存储 JSON 文档，比如 用户信息、博客文章、日志数据 等。
# 你需要存储用户的信息，如 姓名、年龄、技能、地址，并支持 查询和更新。

docker exec -it mongo_container mongosh -u root -p example --authenticationDatabase admin
# 切换到 `users` 数据库
use users


show dbs  // 只会显示有数据的数据库

MongoDB使用"惰性创建"(lazy creation)模式来处理数据库:

无需预先存在：

use users 命令会切换到"users"数据库，不管它是否已经存在
MongoDB不会立即检查该数据库是否存在，也不会立即创建它
首次写入时创建：

数据库只在首次插入数据时才被实际创建
在此之前，数据库名称仅作为当前会话的上下文存在




MongoDB中的 use users 命令说明
这条命令用于切换到名为"users"的数据库，这是必要的原因：


1. 选择工作环境：

MongoDB服务器可以包含多个数据库
use users 告诉MongoDB你要在哪个数据库上执行操作
所有后续查询都会在这个数据库中执行

2. 数据隔离：

用户相关数据通常存储在专门的数据库中
将不同类型的数据（用户、产品、订单等）分开存储
便于管理权限和访问控制

3. 自动创建：

如果"users"数据库不存在，当你首次向其写入数据时，MongoDB会自动创建它


4. 上下文设置：

后续的db.collection.find()等命令需要知道在哪个数据库中执行
不指定数据库将导致操作失败或在默认数据库上执行



# 插入用户数据
db.profiles.insertOne({
    "user_id": 1001,
    "name": "Ananya",
    "age": 25,
    "skills": ["Python", "Docker", "Machine Learning"],
    "address": {
        "city": "Toronto",
        "country": "Canada"
    }
})



# 查询所有用户
db.profiles.find().pretty()

# 查询特定用户
db.profiles.find({ "user_id": 1001 }).pretty()

# 更新用户技能
db.profiles.updateOne({ "user_id": 1001 }, { $push: { "skills": "DevOps" } })

# 删除用户
db.profiles.deleteOne({ "user_id": 1001 })



MongoDB与应用集成及金融行业优势
MongoDB数据与应用集成
MongoDB返回的JSON格式数据与现代应用架构无缝匹配：

1.后端API集成

// Node.js Express后端示例
app.get('/api/users/:id', async (req, res) => {
  const user = await db.profiles.findOne({ user_id: Number(req.params.id) });
  res.json(user); // 直接返回MongoDB文档
});

2.前端应用消费

// React前端示例
fetch('/api/users/1001')
  .then(response => response.json())
  .then(userData => {
    // 直接使用嵌套数据结构
    console.log(`${userData.name} from ${userData.address.city}`);
  });


银行/保险行业优势
客户360°视图

单一文档存储完整客户画像
无需复杂JOIN即可获取全面信息
产品灵活性

轻松适应复杂金融产品结构变化
支持保险计划或银行产品的个性化配置
监管合规

轻松扩展文档字段适应新的合规要求
内置版本控制和审计功能
风险评估

存储多层次风险因素和历史分析
支持复杂嵌套数据结构的实时查询
这种灵活性使金融机构可以快速适应市场变化，而不受传统关系型数据库固定架构的限制。