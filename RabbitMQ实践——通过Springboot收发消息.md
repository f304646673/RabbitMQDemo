@[TOC](大纲)

在[《RabbitMQ实践——在管理后台测试消息收发功能》](https://blog.csdn.net/breaksoftware/article/details/139655228)中，我们在管理后台实践了消息的生产、发送和接收功能。本文我们将使用Springboot框架，实现RabbitMQ消息的收发功能。

# 工程以及依赖
我们先构建出一个包含“Spring for RabbitMQ”的项目。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/5d8165e293db4528a76f0275e343807c.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/51b6bbc95ed849ea84922e39ecfd7713.png)
由于我们还要使用knife4j做可视化的接口测试，于是还要在pom.xml中引入该组件。
最终依赖部分如下

```xml
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-stream</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
			<groupId>com.github.xiaoymin</groupId>
			<artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
			<version>4.5.0</version>
		</dependency>
    </dependencies>
```
# 配置
## RabbitMQ的连接配置
和RabbitMQ相关的配置放在application.properties中
```yaml
spring.rabbitmq.host=192.168.177.43
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admpwd
```
username和password是我们之前在[《RabbitMQ实践——在Ubuntu上安装并启用管理后台》](https://fangliang.blog.csdn.net/article/details/139639464)中配置的。
我们可以在管理后台自己配置一个新的用户。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/c0643b99b0c143e98d6332885a6d6946.png)
然后进入这个新增的用户
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/346ce2550f9040798aef393d62cc86cd.png)
给它配置权限
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b6398f3a19254e80810f89acec2a6690.png)
## knife4j配置
knife4j不是必须的，它只是为了方便我们做接口测试的。
该配置放在application.yml中

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
  group-configs:
    - group: 'Controller'
      paths-to-match: '/**'
      packages-to-scan: com.rabbitmq.basic.controller

knife4j:
  enable: true
  setting:
    language: zh_cn
```
# 代码
## 接收消息
### Service
我们使用RabbitTemplate对象的receive方法，从队列classic_queue中获取消息。
```java
package com.rabbitmq.basic.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetterService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public String getMessage() {
        Message msg = rabbitTemplate.receive("classic_queue");
        if (msg == null) {
            return "No message available";
        }
        return new String(msg.getBody());
    }
}
```
### Controller

```java
package com.rabbitmq.basic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.basic.service.GetterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Getter", description = "The Getter API")
@RestController
@RequestMapping("/getter")
public class GetterController {
    @Autowired
    private GetterService getterService;

    @Operation(summary = "Get a message", description = "Get a message from GetterService")
    @GetMapping("/get")
    public String get() {
        return getterService.getMessage();
    }
    
}
```
### 测试
我们启动程序，可以看到RabbitMQ新增了一条连接，这条连接上一个Channel
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/a25c0fa6c2ce483c8c75528d86b39973.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/af3525fde5734b1998ad712bb4f28d2d.png)
然后我们使用[《RabbitMQ实践——在管理后台测试消息收发功能》](https://fangliang.blog.csdn.net/article/details/139655228)中的方法，先在后台上发送一条消息。
然后在接口测试页面（http://127.0.0.1:8080/doc.html#/Controller/Getter/get）可以获取数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/cc3428439d3d493b8ee6329a906d61c6.png)
## 发送消息
### Service
这次我们使用convertAndSend方法，它的第一个参数是交换器的名称。本例我们使用默认交换器，即名字为空的交换器；第二个参数是Routing Key。由于默认交换器使用绑定关系是：Routing Key中字段值与队列名称匹配，所以我们这个参数传递的是之前创建的队列名；第三个参数是Web端传进来的用户自定义内容。
```java
package com.rabbitmq.basic.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SetterService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend("", "classic_queue", message);
    }
}
```
### Controller

```java
package com.rabbitmq.basic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.basic.service.SetterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Setter", description = "The Setter API")
@RestController
@RequestMapping("/setter")
public class SetterController {
    @Autowired
    private SetterService setterService;

    @Operation(summary = "Send a message", description = "Send a message to SetterService")
    @PostMapping("/send")
    public void send(@RequestParam String message) {
        setterService.sendMessage(message);
    }
    
}

```
### 测试
在http://127.0.0.1:8080/doc.html#/Controller/Setter/send中发送消息到队列中
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b4fa7768ae0d4e59878f655a48558e17.png)
然后在http://127.0.0.1:8080/doc.html#/Controller/Getter/get中获取信息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/13dda20ad8cc44798a77d07acdceacc0.png)

