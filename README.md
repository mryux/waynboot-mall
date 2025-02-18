# waynboot-mall

| 分支名称                                                                           | Spring Boot 版本 | JDK 版本 |
|--------------------------------------------------------------------------------|----------------|--------|
| [master](https://github.com/wayn111/waynboot-mall)                             | 3.1.4          | 17     |
| [springboot-2.7](https://github.com/wayn111/waynboot-mall/tree/springboot-2.7) | 2.7            | 1.8    | 

 
---

- [简介](#简介)
- [系统架构](#系统架构)
- [功能设计](#功能设计)
- [系统设计](#系统设计)
- [接口文档](#接口文档)
- [技术选型](#技术选型)
- [文件目录](#文件目录)
- [todo](#todo)
- [本地开发](#本地开发)
- [在线体验](#在线体验)
- [演示gif](#演示gif)
- [文件目录](#文件目录)
- [感谢](#感谢)

---

# 简介

🔥waynboot-mall 是一套全部开源的H5商城项目，包含**运营后台、H5 商城前台和后端接口三个项目**
。实现了一套完整的商城业务，有首页展示、商品分类、商品详情、sku
详情、商品搜索、加入购物车、结算下单、支付宝/微信支付/易支付对接、我的订单列表、商品评论等一系列功能🔥。

商城所有项目源码全部开源，绝无套路。技术上基于 Spring Boot3.1、Mybatis Plus、Spring Security、Vue2，整合了
Mysql、Redis、RabbitMQ、ElasticSearch、Nginx 等常用中间件，根据我多年线上项目实战经验总结开发而来不断优化、完善。

对于初学者而言 waynboot-mall 项目是非常易于本地开发部署的，根据 readme 中的本地开发指南就能成功启动项目。

并且提供了 docker-compose 服务器一键部署脚本，只需要十多分钟就能在服务器上启动商城前后台所有服务。

- 后端接口项目 https://github.com/wayn111/waynboot-mall
- 前端H5商城项目 https://github.com/wayn111/waynboot-mobile
- 前端运管后台项目 https://github.com/wayn111/waynboot-admin

> 如果有任何使用问题，欢迎提交Issue或加wx告知，方便互相交流反馈～ 💘。最后，喜欢的话麻烦给我个star

# 系统架构

![系统架构](images/系统架构.png)

# 功能设计

![功能设计](images/功能设计.png)

# 系统设计

![系统设计](images/系统设计.png)

关注我的公众号：程序员wayn，专注技术干货输出、分享开源项目。回复关键字：

- **加群**：加群交流，探讨技术问题。
- **演示账号**：获得 waynboot-mall 商城后台演示账号。
- **开源项目**：获取我写的三个开源项目，包含PC、H5商城、后台权限管理系统等。
- **wayn商城资料**：获取wayhboot-mall项目配套资料以及商城图片压缩包下载地址。
- **加微信**：联系我。

<img src="images/wx-mp-code.png" width = "200"  alt="扫码关注公众号"/>

---

# 接口文档
本项目使用 apifox 提供的在线文档功能供大家在线查看以及浏览。

文档地址：https://apifox.com/apidoc/shared-f48b11f5-6137-4722-9c70-b9c5c3e5b09b

![apifox-前台接口.png](images%2Fapifox-%E5%89%8D%E5%8F%B0%E6%8E%A5%E5%8F%A3.png)

![apifox-前台接口.png](images%2Fapifox-%E5%89%8D%E5%8F%B0%E6%8E%A5%E5%8F%A3.png)
---

# 技术选型

|    | 系统组件              | 采用技术                        | 官网                                                                                         |
|----|-------------------|-----------------------------|--------------------------------------------------------------------------------------------|
| 1  | 基础框架              | Spring Boot                 | https://spring.io/projects/spring-boot                                                     |
| 2  | ORM 框架            | MyBatis-Plus                | https://baomidou.com                                                                       |
| 3  | 工具类库              | hutool                      | https://hutool.cn                                                                          |
| 4  | 流量网关、网关安全         | openresty                   | https://openresty.org/cn/                                                                  |
| 5  | 访问控制              | Spring Security             | https://spring.io/projects/spring-security                                                 |
| 6  | 日志记录              | logback                     | https://logback.qos.ch/                                                                    |
| 7  | 验证码               | easy-captcha                | https://github.com/ele-admin/EasyCaptcha                                                   |
| 8  | 数据库连接池            | HikariCP                    | https://github.com/brettwooldridge/HikariCP                                                |
| 9  | Redis 客户端         | Lettuce                     | https://lettuce.io                                                                         |
| 10 | Elasticsearch 客户端 | Java High Level REST Client | https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high.html |
| 11 | 消息队列              | RabbitMQ                    | https://www.rabbitmq.com                                                                   |
| 12 | 定时任务              | xxl-job                     | https://www.xuxueli.com/xxl-job                                                            |
| 13 | 服务监控              | spring-boot-admin           | https://docs.spring-boot-admin.com/current/getting-started.html                            |

---

# todo

- [x] 订单详情页面
- [x] 完善支付功能
- [ ] 商城资讯流
- [ ] 联系客服

---

# 本地开发

由于本项目图片压缩包超过 100m 不能在 github 上传，所以下载链接放在我的公众号【程序员wayn】，
回复 wayn商城图片 获取

## 1. 克隆项目

git clone git@github.com:wayn111/waynboot-mall.git

## 2. 导入项目依赖

将 waynboot-mall 目录用 idea 打开，导入 maven 依赖

## 3. 安装 Jdk17、Mysql8.0+、Redis3.0+、RabbitMQ3.0+（含延迟消息插件）、ElasticSearch7.0+（含分词、拼英插件）到本地

## 4. 导入 sql 文件

在项目根目录下，找到 `wayn_shop_*.sql` 文件，新建 mysql 数据库 wayn_shop，导入其中

## 5. 项目图片部署

下载商城图片压缩包，将 zip 中所有图片解压缩部署到 D:/waynshop/webp 目录下，如下

![图片部署.png](images/图片部署.png)

## 6. 修改Mysql、Redis、RabbitMQ、Elasticsearch连接配置

修改`application-dev.yml`以及`application.yml`文件中数据连接配置相关信息

## 7. 启动项目

- 后台api：
  进入waynboot-admin-api子项目，找到AdminApplication文件，右键`run AdminApplication`，启动后台项目
- h5商城api:
  进入waynboot-mobile-api子项目，找到MobileApplication文件，右键`run MobileApplication`，启动h5商城项目
- 消费者api：
  进入waynboot-message-consumer子项目，找到MessageApplication文件，右键`run MessageApplication`，启动消费者项目

## 8. 启动商城H5项目

请查看商城H5前端项目 https://github.com/wayn111/waynboot-mobile ，readme文档，进行本地启动

## 9. 启动商城后管项目

请查看商城后管前端项目 https://github.com/wayn111/waynboot-admin ，readme文档，进行本地启动

---

# 在线体验

前台

- 使用邮箱 + 手机号注册商城用户
- 使用手机号 + 密码登陆

演示地址以及账号：关注我的公众号【程序员wayn】，发送 演示账号

---

# 服务器部署

对于想要自己部署这个项目的同学又没有开发资源，可以关注公众号【程序员wayn】 发送 **加微信**，提供有偿帮助。

# 咨询指南

商城咨询时请考虑我的时间成本，虽然我是乐于帮助新手解决问题。

但是某些人不仅白嫖咨询大量问题，消耗我的时间成本。而且咨询态度就像是我的客户一样，咨询完了一句谢谢也不会说。

所以如有咨询大量问题，请先付出金钱成本😜。

# 演示gif

## 前台演示

![mall.gif](images/mall.gif)

## 后台演示

![admin.gif](images/admin.gif)

# 文件目录

```
|-- db-init                           // 数据库初始化脚本
|-- waynboot-monitor                  // 监控模块
|-- waynboot-util                     // 帮助模块，包含项目基础帮助类 
|   |-- constant                      // 基础常量
|   |-- converter                     // 基础转换类
|   |-- enums                         // 基础枚举
|   |-- exception                     // 基础异常
|   |-- util                          // 基础帮助类
|-- waynboot-admin-api                // 运营后台api模块，提供后台项目api接口
|   |-- controller                    // 后台接口
|   |-- framework                     // 后台配置相关
|-- waynboot-common                   // 通用模块，包含项目核心基础类
|   |-- annotation                    // 通用注解      
|   |-- base                          // 通用注解
|   |-- core                          // 核心配置，包含项目entity、mapper、service、vo定义
|   |-- config                        // 通用配置
|   |-- design                        // 设计模式实现
|   |-- dto                           // dto定义
|   |-- request                       // 接口请求定义
|   |-- reponse                       // 接口响应定义
|   |-- task                          // 任务相关   
|   |-- util                          // 通用帮助类   
|   |-- wapper                        // 通用包装类，包含易支付代码
|-- waynboot-data                     // 数据模块，通用中间件数据访问
|   |-- waynboot-data-redis           // redis访问配置模块
|   |-- waynboot-data-elastic         // elastic访问配置模块
|-- waynboot-message                  // 消息模块，rabbitmq操作
|   |-- waynboot-message-core         // rabbitmq消息配置，定义队列、交换机、绑定队列到交换机
|   |-- waynboot-message-consumer     // rabbitmq消费者，消费消息
|-- waynboot-mobile-api               // H5商城api模块，提供H5商城api接口
|   |-- controller                    // 前台接口
|   |-- framework                     // 前台配置相关
|-- pom.xml                           // maven父项目依赖，定义子项目依赖版本
|-- ...
```

# 感谢

- [panda-mall](https://github.com/Ewall1106/vue-h5-template)
- [litemall](https://github.com/linlinjava/litemall)
- [vant-ui](https://github.com/youzan/vant)

# 捐助

<img src="./images/捐助.jpg" width="260" alt="如果这个项目对你有所帮助，不如请作者喝杯咖啡吧">
