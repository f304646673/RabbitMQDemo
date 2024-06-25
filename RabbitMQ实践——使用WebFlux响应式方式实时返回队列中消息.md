@[TOC](大纲)

在之前的案例中，我们在管理后台收发消息都是通过短连接的形式。本文我们将探索对队列中消息的实时读取，并通过流式数据返回给客户端。
webflux是反应式Web框架，客户端可以通过一个长连接和服务端相连，后续服务端可以通过该连接持续给客户端发送消息。可以达到：发送一次，多次接收的效果。
# Pom.xml
由于我们要使用Rabbitmq，所以要新增如下依赖

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-amqp</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.amqp</groupId>
			<artifactId>spring-rabbit-stream</artifactId>
		</dependency>
```
webflux的依赖如下：

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webflux</artifactId>
		</dependency>
		<dependency>
			<groupId>io.projectreactor</groupId>
			<artifactId>reactor-core</artifactId>
			<version>3.6.7</version>
		</dependency>
```
# 监听队列
下面代码会返回一个监听队列的Container
```java
    private SimpleMessageListenerContainer getListener(String queueName, MessageListener messageListener) {
        lock.lock();
        try {
            SimpleMessageListenerContainer listener = listeners.get(queueName);
            if (listener == null && messageListener != null) {
                listener = new SimpleMessageListenerContainer();
                listener.setConnectionFactory(connectionFactory);
                listener.setQueueNames(queueName);
                listener.setMessageListener(messageListener);
                listeners.put(queueName, listener);
            }
            return listener;
        } finally {
            lock.unlock();
        }
    }
```
# 实时返回消息
一旦消费者读取到消息，onMessage方法会被调用。然后Flux的消费者会将消息投递到流上。
```java
    public Flux<String> listen(String queueName) {
       return Flux.create(emitter -> {
           SimpleMessageListenerContainer container = getListener(queueName, new MessageListener() {
               @Override
               public void onMessage(Message message) {
                   String msg = new String(message.getBody());
                   System.out.println("listen function Received message: " + msg);
                   emitter.next(msg);
               }
           });
           container.start();
       });
    }
```
# 测试
由于OpenApi不能支持实时展现流式数据，所以我们采用Postman来测试。
发送请求后，该页面一直处于滚动状态。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4339a7a38f304c42a8830fb430b2b5d3.png)
在管理后台发送一条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0c4da0139c974d25b7ca92d9bce68ecc.png)
可以看到Postman收到了该消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/05653c31582745148a19cff1a4460082.png)
然后在发一条，Postman又会收到一条
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0e4aa279c84741ca9f6a46b76c04a0a2.png)
这样我们就完成了“请求一次，多次返回”的效果。
# 完整代码
需要注意的是，返回的格式需要标记为produces = "text/event-stream"。
```java
// controller
package com.rabbitmq.consumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.consumer.service.ConsumerService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    
    @Autowired
    private ConsumerService comsumerService;

   
    @GetMapping(value = "/listen", produces = "text/event-stream")
    public Flux<String> listen(@RequestParam String queueName) {
        return comsumerService.listen(queueName);
    }
}
```

```java
// service
package com.rabbitmq.consumer.service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Service
public class ConsumerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private ConnectionFactory connectionFactory;

    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, SimpleMessageListenerContainer> listeners = new java.util.HashMap<>();

    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
    }

    public Flux<String> listen(String queueName) {
       return Flux.create(emitter -> {
           SimpleMessageListenerContainer container = getListener(queueName, new MessageListener() {
               @Override
               public void onMessage(Message message) {
                   String msg = new String(message.getBody());
                   System.out.println("listen function Received message: " + msg);
                   emitter.next(msg);
               }
           });
           container.start();
       });
    }

    private SimpleMessageListenerContainer getListener(String queueName, MessageListener messageListener) {
        lock.lock();
        try {
            SimpleMessageListenerContainer listener = listeners.get(queueName);
            if (listener == null && messageListener != null) {
                listener = new SimpleMessageListenerContainer();
                listener.setConnectionFactory(connectionFactory);
                listener.setQueueNames(queueName);
                listener.setMessageListener(messageListener);
                listeners.put(queueName, listener);
            }
            return listener;
        } finally {
            lock.unlock();
        }
    }
}
```
# 工程代码
[https://github.com/f304646673/RabbitMQDemo/tree/main/consumer](https://github.com/f304646673/RabbitMQDemo/tree/main/consumer)
