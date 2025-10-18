# 商品共购关系分析平台 - API文档

## 📋 项目概述

**项目名称**：Amazon商品共购关系分析平台  
**后端技术栈**：Spring Boot 3.5.6 + Java 17  
**数据来源**：Stanford SNAP亚马逊数据集（清洗后100个节点，353条边）  
**主要功能**：商品共购关系网络可视化与分析

---

## 🚀 基础信息

### 服务地址
- **开发环境**：`http://localhost:8080`
- **API前缀**：`/api/network`
- **完整基础URL**：`http://localhost:8080/api/network`

### 请求/响应格式
- **请求格式**：HTTP GET
- **响应格式**：JSON
- **字符编码**：UTF-8

### 跨域配置
- **允许源**：`http://localhost:3000`（React开发服务器）
- **允许方法**：GET, POST, PUT, DELETE, OPTIONS
- **允许凭证**：是

---

## 🔌 核心API接口

### 1. 健康检查接口
**用途**：检查后端服务状态

- **URL**: `GET /health`
- **响应示例**：
```json
"✅ 商品共购关系分析平台后端服务运行正常"
```
- **测试地址**：http://localhost:8080/api/network/health

### 2. 获取完整网络数据
**用途**：初始化力导向图，包含所有节点和边数据

- **URL**: `GET /full`
- **响应结构**：
```json
{
  "nodes": [
    {
      "id": 0,
      "asin": "0771044445",
      "title": "",
      "group": "Unknown",
      "degree": 7
    },
    // ... 更多节点
  ],
  "links": [
    {
      "source": 0,
      "target": 1
    },
    // ... 更多边
  ],
  "metadata": {
    "totalNodes": 100,
    "totalEdges": 353,
    "cleanedAt": "2025-10-18T14:44:12.033218"
  }
}
```
- **测试地址**：http://localhost:8080/api/network/full

### 3. 获取网络统计信息
**用途**：显示数据概览和统计面板

- **URL**: `GET /statistics`
- **响应结构**：
```json
{
  "totalNodes": 100,
  "totalEdges": 353,
  "cleanedAt": "2025-10-18T14:44:12.033218",
  "groupDistribution": {
    "Book": 85,
    "Music": 10,
    "DVD": 3,
    "Video": 2
  },
  "averageDegree": 7.06,
  "maxDegree": 20,
  "minDegree": 1
}
```
- **测试地址**：http://localhost:8080/api/network/statistics

### 4. 获取所有商品类别
**用途**：构建分类筛选器

- **URL**: `GET /groups`
- **响应示例**：
```json
["Book", "Music", "DVD", "Video"]
```
- **测试地址**：http://localhost:8080/api/network/groups

### 5. 根据类别筛选商品
**用途**：按商品类别过滤节点

- **URL**: `GET /nodes/group/{group}`
- **参数**：
  - `group` (路径参数)：商品类别，如 "Book", "Music"
- **响应示例**：
```json
[
  {
    "id": 1,
    "asin": "0827229534",
    "title": "Patterns of Preaching: A Sermon Sampler",
    "group": "Book",
    "degree": 6
  }
  // ... 该类别下所有商品
]
```
- **测试地址**：http://localhost:8080/api/network/nodes/group/Book

### 6. 获取节点详情
**用途**：显示单个商品的详细信息

- **URL**: `GET /nodes/{id}`
- **参数**：
  - `id` (路径参数)：节点ID，0-99
- **响应示例**：
```json
{
  "id": 1,
  "asin": "0827229534",
  "title": "Patterns of Preaching: A Sermon Sampler",
  "group": "Book",
  "degree": 6
}
```
- **测试地址**：http://localhost:8080/api/network/nodes/1

### 7. 获取热门商品（高度连接）
**用途**：显示连接度最高的商品（最受欢迎）

- **URL**: `GET /nodes/highly-connected?limit=10`
- **参数**：
  - `limit` (查询参数，可选)：返回数量，默认10
- **响应示例**：
```json
[
  {
    "id": 8,
    "asin": "0231118597",
    "title": "Losing Matt Shepard",
    "group": "Book",
    "degree": 20
  }
  // ... 按连接度降序排列
]
```
- **测试地址**：http://localhost:8080/api/network/nodes/highly-connected?limit=5

### 8. 获取邻居节点
**用途**：高亮显示与指定商品相关的商品

- **URL**: `GET /nodes/{id}/neighbors`
- **参数**：
  - `id` (路径参数)：中心节点ID
- **响应示例**：
```json
[
  {
    "id": 0,
    "asin": "0771044445",
    "title": "",
    "group": "Unknown",
    "degree": 7
  }
  // ... 所有邻居节点
]
```
- **测试地址**：http://localhost:8080/api/network/nodes/1/neighbors

### 9. 搜索商品
**用途**：根据关键词搜索商品

- **URL**: `GET /nodes/search?keyword={keyword}`
- **参数**：
  - `keyword` (查询参数)：搜索关键词
- **响应示例**：
```json
[
  {
    "id": 1,
    "asin": "0827229534",
    "title": "Patterns of Preaching: A Sermon Sampler",
    "group": "Book",
    "degree": 6
  }
  // ... 匹配的商品
]
```
- **测试地址**：http://localhost:8080/api/network/nodes/search?keyword=book

---

## 📊 数据模型说明

### 节点对象 (Node)
```typescript
interface Node {
  id: number;        // 节点唯一标识 (0-99)
  asin: string;      // 亚马逊商品编号
  title: string;     // 商品标题（已清洗，最大80字符）
  group: string;     // 商品类别：Book, Music, DVD, Video
  degree: number;    // 连接度（与其他商品的关联数量）
}
```

### 边对象 (Link)
```typescript
interface Link {
  source: number;    // 源节点ID
  target: number;    // 目标节点ID
}
```

### 元数据对象 (Metadata)
```typescript
interface Metadata {
  totalNodes: number;    // 总节点数：100
  totalEdges: number;    // 总边数：353
  cleanedAt: string;     // 数据清洗时间（ISO格式）
}
```

### 完整网络数据 (NetworkData)
```typescript
interface NetworkData {
  nodes: Node[];        // 所有节点
  links: Link[];        // 所有边
  metadata: Metadata;   // 元数据
}
```

### 统计信息 (Statistics)
```typescript
interface Statistics {
  totalNodes: number;           // 总节点数
  totalEdges: number;           // 总边数
  cleanedAt: string;            // 清洗时间
  groupDistribution: {          // 类别分布
    [group: string]: number;
  };
  averageDegree: number;        // 平均连接度
  maxDegree: number;            // 最大连接度
  minDegree: number;            // 最小连接度
}
```

---


---

## 🔧 错误处理

### 常见HTTP状态码
- `200`：请求成功
- `404`：资源未找到（如节点ID不存在）
- `500`：服务器内部错误

### 错误响应格式
```json
{
  "timestamp": "2025-10-18T15:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "未找到节点: 999",
  "path": "/api/network/nodes/999"
}
```

---

## 📈 性能说明

### 数据规模
- **节点数量**：100个商品
- **边数量**：353条共购关系

### 数据特征
- **商品类别分布**：主要包含Book、Music、DVD、Video等类别
- **连接度范围**：1-20个连接关系
- **数据完整性**：所有字段均有值，无缺失数据
