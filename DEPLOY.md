# 宝塔 Docker 部署指南

## 前置条件

- 宝塔面板已安装
- 宝塔 Docker 管理器已安装（应用商店 → Docker管理器）
- 服务器开放 80 端口

## 部署步骤

### 1. 上传项目到服务器

```bash
# 方式一：Git 拉取
cd /www/wwwroot
git clone https://gitee.com/superWang96121/zhinenggongdanxitong.git
cd zhinenggongdanxitong

# 方式二：宝塔面板上传压缩包后解压
```

### 2. 创建环境配置

```bash
cd /www/wwwroot/zhinenggongdanxitong
cp .env.example .env
```

编辑 `.env` 填入实际配置：

```bash
vi .env
```

```properties
DB_NAME=reg_zhishiku
DB_USERNAME=root
DB_PASSWORD=你的数据库密码

JWT_SECRET=YWRtaW5fc3lzdGVtX3NlY3JldF9rZXlfMjAyNF92ZXJ5X2xvbmdfYW5kX3NlY3VyZQ==

AI_BASE_URL=https://你的API地址/v1
AI_API_KEY=你的API密钥
AI_MODEL_NAME=deepseek-v3.2
```

同时创建后端 `.env`（本地开发用的那份）：

```bash
cp backend/.env.example backend/.env
# 编辑填入相同配置，DB_HOST 改为 mysql（Docker 内部网络）
```

### 3. 构建并启动

```bash
cd /www/wwwroot/zhinenggongdanxitong
docker compose up -d --build
```

首次构建大约需要 5-10 分钟（下载依赖）。

### 4. 查看运行状态

```bash
docker compose ps
docker compose logs -f
```

应看到 3 个容器运行中：

| 容器 | 端口 | 说明 |
|------|------|------|
| gongdan-mysql | 3307:3306 | MySQL 数据库 |
| gongdan-backend | (内部8080) | Spring Boot 后端 |
| gongdan-frontend | 80:80 | Nginx + Vue 前端 |

### 5. 访问系统

浏览器打开：`http://你的服务器IP`

| 用户名 | 密码 |
|--------|------|
| admin | admin123 |

## 宝塔反向代理（可选：绑定域名 + HTTPS）

如果需要绑定域名和 SSL：

1. 宝塔面板 → 网站 → 添加站点 → 填入域名
2. 站点设置 → 反向代理 → 添加反向代理：
   - 目标 URL：`http://127.0.0.1:80`
   - 发送域名：`$host`
3. 站点设置 → SSL → 申请 Let's Encrypt 证书

注意：如果宝塔 Nginx 占用了 80 端口，需修改 `docker-compose.yml` 中前端端口：

```yaml
frontend:
  ports:
    - "8088:80"  # 改为其他端口
```

然后宝塔反向代理目标 URL 改为 `http://127.0.0.1:8088`。

## 常用运维命令

```bash
# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 重启服务
docker compose restart backend

# 更新部署（拉取新代码后）
git pull
docker compose up -d --build

# 停止所有服务
docker compose down

# 停止并清除数据（谨慎！）
docker compose down -v

# 进入后端容器调试
docker exec -it gongdan-backend sh

# 进入 MySQL
docker exec -it gongdan-mysql mysql -uroot -p
```

## 常见问题

### Q: 后端启动失败，提示连不上数据库

MySQL 容器需要约 30 秒初始化。`docker-compose.yml` 已配置了 `healthcheck`，后端会等待 MySQL 就绪后再启动。如果仍然失败：

```bash
docker compose restart backend
```

### Q: AI 问答无响应

检查 `.env` 中的 `AI_BASE_URL` 和 `AI_API_KEY` 是否正确。从服务器测试 API 连通性：

```bash
curl -H "Authorization: Bearer 你的KEY" 你的BASE_URL/models
```

### Q: 上传的文件找不到

文件存储在 Docker volume `uploads_data` 中，不会因为重新构建丢失。
