临时队列是一种自动删除队列。当这个队列被创建后，如果没有消费者监听，则会一直存在，还可以不断向其发布消息。但是一旦的消费者开始监听，然后断开监听后，它就会被自动删除。
# 新建自动删除队列
我们创建一个名字叫queue.auto.delete的临时队列
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/a1f94429aca14e2f8d2d05f1d63741cb.png)
# 绑定
我们直接使用默认交换器，所以不用创建新的交换器，也不用建立绑定关系。
# 实验
## 发布消息
我们在后台管理页面的默认交换器下向这个队列发布2条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/dbc2bf18bc064e2ea8a534a46a0add64.png)
## 监听队列
这次我们需要使用代码来订阅队列

```java
package com.rabbitmq.basic.service;

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

    public void startListening(String queueName) {
        SimpleMessageListenerContainer container = getListener(queueName, new RBMQMessageListener());
        container.start();
        System.out.println("Listening to " + queueName);
    }
    
    public void stopListening(String queueName) {
        SimpleMessageListenerContainer container = getListener(queueName, null);
        container.stop();
        System.out.println("Stopped listening to " + queueName);
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

    private class RBMQMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            System.out.println("Received message: " + new String(message.getBody()));
        }
    }

}

```
然后在页面上开启监听
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1158e87a833e423f8a99fe484a7acb18.png)

可以看到终端上输出了之前发布的消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/37794caade494d3ea4d89ef983755d98.png)
然后我们继续在后台发布3条消息，可以看到新的消息也被接收到。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ab763d0557a54aa3a3ec5b5d88040be5.png)
然后我们关闭监听
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6a2fe6131ead455294e364baeca20012.png)

队列queue.auto.delete就被删除掉了
