@[TOC](大纲)

经过之前的若干节的学习，我们基本掌握了Rabbitmq各个组件和功能。本文我们将使用之前的知识搭建一个简单的单人聊天服务。
基本结构如下。为了避免Server有太多连线导致杂乱，下图将Server画成两个模块，实则是一个服务。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ff208d88857e46eb89d1eef4cac929d4.png)
该服务由两个核心交换器构成。
Core交换器是服务启动时创建的，它主要是为了向不同用户传递“系统通知型”消息。比如Jerry向Tom发起聊天邀请，则是通过上面黑色字体6-10的流程发给了Core交换器。然后Core交换器将该条消息告知Tom。
Fanout交换器是用来消息传递的。Jerry和Tom都向其发送消息，然后路由到两个队列。它们两各自订阅一个队列，就可以看到彼此的聊天内容了。
# 创建Core交换器

```java
package com.rabbitmq.chat.service;

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
public class Core {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private ConnectionFactory connectionFactory;

    final String exchangeName = "Core";

    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
        createExchange(exchangeName);
    }

    private void createExchange(String exchangeName) {
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare(exchangeName, "direct", false, true, null);
            return null;
        });
    }
```
# 用户登录
用户登录后，我们会创建一个“系统通知”队列。然后用户就会通过长连接形式，持续等待系统发出通知。

```java
    private final ReentrantLock lock = new ReentrantLock();
    final private Map<String, SimpleMessageListenerContainer> listeners = new java.util.HashMap<>();
    
    public Flux<String> Login(String username) {
        createExclusiveQueue(username);
        createBanding(exchangeName, username, username);
        return Flux.create(emitter -> {
           SimpleMessageListenerContainer container = getListener(username, (Message message) -> {
               String msg = new String(message.getBody());
               System.out.println("Received message: " + msg);
               emitter.next(msg);
           });
           container.start();
       });
    }
 
     private void createExchange(String exchangeName) {
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare(exchangeName, "direct", false, true, null);
            return null;
        });
    }

    private void createBanding(String exchangeName, String queueName, String routingKey) {
        rabbitTemplate.execute(channel -> {
            channel.queueBind(queueName, exchangeName, routingKey);
            return null;
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
```
Controller如下

```java
package com.rabbitmq.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.chat.service.Core;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private Core core;

    @PostMapping(value = "/login", produces = "text/event-stream")
    public Flux<String> login(@RequestParam String username) {
        return core.Login(username);
    }
}
```
#  发起聊天邀请
发起聊天邀请时，系统会预先创建一个聊天室（ChatRoomInfo ）。它包含上图中Fanout交换器、以及聊天双方需要订阅的消息队列。
这些创建完后，发起方就会等待对方发送的消息，也可以自己和自己聊天。因为消息队列已经创建好了，只是对方还没使用。
```java
package com.rabbitmq.chat.service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import reactor.core.publisher.Flux;

@Service
public class ChatRoom {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private ConnectionFactory connectionFactory;

    @Data
    private class ChatRoomInfo {
        private String exchange;
        private Map<String, String> usernameToQueuename;
    }

    private final Map<String, ChatRoomInfo> chatRooms = new java.util.HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();   
    
    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
    }

    public Flux<String> invite(String fromUsername, String toUsername) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        ChatRoomInfo chatRoomInfo = chatRooms.get(chatRoomName);
        if (chatRoomInfo == null) {
            createChatRoom(fromUsername, toUsername);
        }
        return talk(chatRoomName, fromUsername);
    }
    
    private void createChatRoom(String fromUsername, String toUsername) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        String exchangeName = chatRoomName;
        String fromQueueName = "queue-" + fromUsername + "-" + toUsername;
        String toQueueName = "queue-" + toUsername + "-" + fromUsername;
        
        rabbitTemplate.execute(action -> {
            action.exchangeDeclare(exchangeName, "fanout", false, true, null);
            action.queueDeclare(fromQueueName, false, true, false, null);
            action.queueDeclare(toQueueName, false, true, false, null);
            action.queueBind(fromQueueName, exchangeName, "");
            action.queueBind(toQueueName, exchangeName, "");
            return null;
        });

        lock.lock();
        try {
            ChatRoomInfo chatRoomInfo = new ChatRoomInfo();
            chatRoomInfo.setExchange(exchangeName);
            chatRoomInfo.setUsernameToQueuename(Map.of(fromUsername, fromQueueName, toUsername, toQueueName));
            chatRooms.put(chatRoomName, chatRoomInfo);
        } finally {
            lock.unlock();
        }
    }
```

# 接受邀请
被邀请方通过Core交换器得知有人要和它聊天。
然后接受邀请的请求会寻找聊天室信息，然后订阅聊天记录队列。
```java
    public Flux<String> accept(String fromUsername, String toUsername) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        return talk(chatRoomName, toUsername);
    }

    private Flux<String> talk(String chatRoomName, String username) {
        ChatRoomInfo chatRoomInfo = chatRooms.get(chatRoomName);
        if (chatRoomInfo == null) {
            throw new IllegalArgumentException("Chat room not found");
        }
        String queueName = chatRoomInfo.getUsernameToQueuename().get(username);
        return Flux.create(emitter -> {
            SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer();
            listener.setConnectionFactory(connectionFactory);
            listener.setQueueNames(queueName);
            listener.setMessageListener((Message message) -> {
                String msg = new String(message.getBody());
                System.out.println(username + " received message: " + msg);
                emitter.next(msg);
            });
            listener.start();
        });
    }
```
# 聊天
聊天的逻辑就是找到聊天室信息，然后向交换器发送消息。
```java
    public void chat(String fromUsername, String toUsername, String message) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        ChatRoomInfo chatRoomInfo = chatRooms.get(chatRoomName);
        if (chatRoomInfo == null) {
            chatRoomName = getChatRoomName(toUsername, fromUsername);
            chatRoomInfo = chatRooms.get(chatRoomName);
        }
        if (chatRoomInfo == null) {
            throw new IllegalArgumentException("Chat room not found");
        }
        rabbitTemplate.convertAndSend(chatRoomInfo.getExchange(), "", fromUsername + ": " + message);
    }
    
    private String getChatRoomName(String fromUsername, String toUsername) {
        return fromUsername + "-" + toUsername + "-chat-room";
    }
```
Controller侧代码

```java
package com.rabbitmq.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.chat.service.ChatRoom;
import com.rabbitmq.chat.service.Core;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private Core core;

    @Autowired
    private ChatRoom chatRoom;

    @PutMapping(value = "/invite", produces = "text/event-stream")
    public Flux<String> invite(@RequestParam String fromUsername, @RequestParam String toUsername) {
        core.invite(fromUsername, toUsername);
        return chatRoom.invite(fromUsername, toUsername);
    }

    @PutMapping(value = "/accept", produces = "text/event-stream")
    public Flux<String> accept(@RequestParam String fromUsername, @RequestParam String toUsername) {
        core.accept(fromUsername, toUsername);
        return chatRoom.accept(fromUsername, toUsername);
    }

    @PostMapping("/send")
    public void send(@RequestParam String fromUsername, @RequestParam String toUsername, @RequestParam String message) {
        chatRoom.chat(fromUsername, toUsername, message);
    }
}
```

# 实验过程
在Postman中，我们先让tom登录，然后jerry登录。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/3e3224771c0b45a9af35c486bc4e93c5.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ea932296ff2c4d59aece7732bdc95ea6.png)
在后台，我们看到创建两个队列
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ea375aa714ba424aa377c164b9fe5f39.png)
以及Core交换器的绑定关系也被更新
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d93e2491d3504a7b9b893bc43ca2a92e.png)
Jerry向Tom发起聊天邀请
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0d5757068d4e496bb805e99e4ae09a78.png)
可以看到Tom收到了邀请
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/be989ce1c4cc43fd8d4b75b189cb499a.png)
同时新增了两个队列
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ee1d9bb3049743328c07627c37bc8d22.png)
以及一个交换器
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/de490813371b43c88d28e2b2cccd29e2.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/701e4739ab124b58909c9805adde09e5.png)
Tom通过下面请求接受邀请
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/a0d84799710543638128fe20b044691b.png)
Jerry收到Tom接受了邀请的通知
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/deeca5b73a464ea798267a7bbb08a3a3.png)
后面它们就可以聊天了
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/3a00d570f7894c0db3db9920c9186787.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/80a863eb20884bfab33a7b6782cf5e59.png)
它们的聊天窗口都收到了消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1de0052cc6884952b01f1c2e5f7e22ab.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ab00a2efdc2141dba5710430f075380f.png)
# 总结
本文主要使用的知识点：
* direct交换器以及其绑定规则
* fanout交换器
* 自动删除的交换器
* 自动删除的队列
* 只有一个消费者的队列
* WebFlux响应式编程

