@[TOC](大纲)

在实际工作中，我们往往可能因为代码逻辑的一些问题导致发布的消息无法被路由，比如写错了Routing key。那么这个时候，RabbitMQ可能会拒绝接收，还可以通过其他机制把它收集起来。本文我们将探讨这类问题的处理方法。
# 无备用（Alternate）交换器
一般我们默认创建的交换器都是没有备用（Alternate）交换器的。
## 构建交换器、队列和绑定关系
我们给新建的交换器取名：direct.without.alternate。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/21646df31abd4872a622225a4b065c96.png)
然后创建一个名字叫direct_queue的队列
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f5eff721467f4154adf1cff441d79651.png)
最后创建它们的绑定关系。Routing key是to.direct.queue。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7a514522b0044f6cb9ec19050057c802.png)
## 测试代码
RabbitMQ的Client端和Server端通信时，是在一个连接(connection)上建立了一个双工通道（channel），即Client端可以给Server发消息，Server端也可以给Client端发消息。
当Client端向Server端发布一条消息后，Server端会根据Client端发送来的AMQP协议，决定是否要给告知Client端它对这条消息的处理结果。
如果Client设置了Mandatory为true，在消息没法被路由的情况下，Server端会通过告知Client端“NO_ROUTE”的处理结果，并将消息返还给Client端。如果消息被路由成功，则不会告知Client端。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/e431d178cea04c288f01ae1421217bc6.png)


```java
private org.springframework.amqp.rabbit.core.RabbitTemplate createRabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, boolean mandatory) {
     org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate = null;
     if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
         cachingConnectionFactory.setPublisherReturns(true);
         rabbitTemplate = new org.springframework.amqp.rabbit.core.RabbitTemplate(cachingConnectionFactory);
     } else {
         throw new IllegalArgumentException("The provided connectionFactory is not an instance of CachingConnectionFactory");
     }

     rabbitTemplate.setMandatory(mandatory);
     rabbitTemplate.setReturnsCallback(returned -> {
         System.out.println(STR."Message returned: \{returned.getReplyText()} \n message: \{new String(returned.getMessage().getBody())} \n correlation id: \{returned.getMessage().getMessageProperties().getCorrelationId()} \n routing key: \{returned.getRoutingKey()} \n exchange: \{returned.getExchange()} \n reply code: \{returned.getReplyCode()}"
         );
     });
     return rabbitTemplate;
 }
```
上述代码中setReturnsCallback方法设置了一个回调函数。当mandatory被设置为true，且消息无法被路由时，这个回调会被调用。
需要注意的是：发布消息和回调是发生在不同的过程中，它们是异步的关系。
### 抛弃不可路由消息
我们只要给Mandatory设置为false，则表示RabbitMQ可以接收不可路由消息，但是会将其抛弃。
```java
@Bean(name = "rabbitTemplateWithoutMandatory")
public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplateWithoutMandatory(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
    return createRabbitTemplate(connectionFactory, false);
}
```

```java
@Service
public class NonmandatoryService {

    @Resource(name = "rabbitTemplateWithoutMandatory")
    private RabbitTemplate rabbitTemplateWithoutMandatory;

    public void send(String exchangeName, String routingKey, String message) {
        String msgId = UUID.randomUUID().toString();
        Message msg = MessageBuilder.withBody(message.getBytes())
                .setContentType("text/plain")
                .setCorrelationId(msgId)
                .setMessageId(msgId)
                .build();

        CorrelationData correlationData = new CorrelationData(msgId);
        rabbitTemplateWithoutMandatory.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }
    
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/dd893d0935e24e2189a604016bbf9208.png)

发送完消息后，队列中是看不到这条消息的。而且上述回调也没执行。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9821d1d82bea448da9664148bcd15561.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/026df05c6cca401789073e54e80dfe18.png)
### 返还不可路由消息
当我们设置了Mandatory为true时，在“不可路由”的场景下，回调会被调用。
```java
@Bean(name = "rabbitTemplateWithMandatory")
public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplateWithMandatory(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
    return createRabbitTemplate(connectionFactory, true);
}
```

```java
@Service
public class MandatoryService {
    @Resource(name = "rabbitTemplateWithMandatory")
    private RabbitTemplate rabbitTemplateWithMandatory;

    public void send(String exchangeName, String routingKey, String message) {
        String msgId = UUID.randomUUID().toString();
        Message msg = MessageBuilder.withBody(message.getBytes())
                .setContentType("text/plain")
                .setCorrelationId(msgId)
                .setMessageId(msgId)
                .build();

        CorrelationData correlationData = new CorrelationData(msgId);
        rabbitTemplateWithMandatory.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/19a07c5b1b424458af83754f5729755f.png)

发送完消息后，队列中是看不到这条消息的。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9821d1d82bea448da9664148bcd15561.png)
但是上述回调函数会被执行。“不可路由”的错误码是312。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8f5c8a3f2b1743a8becb56f75ae3281a.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7807d813383d44cfb121f5dafab60ef4.png)

# 有备用（Alternate）交换器
在实际应用中，抛弃掉不可路由信息会让我们忽视掉没有注意到的意外情况，从而影响系统的正确性。但是如果我们开启mandatory功能，则会要求Client端来处理这些意外情况，导致业务系统复杂度增加。
一种比较优雅的解决方案是使用备用交换器，将这些意外信息存储到其他队列中，稍后再做分析和处理。
那备用交换器选用什么类型的呢？在[《RabbitMQ实践——交换器（Exchange）和绑定（Banding）》](https://blog.csdn.net/breaksoftware/article/details/139663306)中，我们介绍了各个交换器的特点。但是发现只有fanout类型交换器可以无条件路由消息，其他都要做匹配工作。所以fanout交换器是适合做备用交换器的。
## 创建带备用（Alternate）交换器的交换器
我们使用在[《RabbitMQ实践——交换器（Exchange）和绑定（Banding）》](https://blog.csdn.net/breaksoftware/article/details/139663306)中介绍的系统自建的amq.fanout作为备用交换器。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ad37abd7c90f4b79a7e4ecf6dc758515.png)
也构建和之前的绑定关系
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/84d95b061b6840898ff362ce09c117a8.png)

然后给这个交换器发布消息。
## mandatory
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ce4e8242842c4359aec46bddd8cafd05.png)
即使开启了mandatory模式，由于备用交换器的存在，RabbitMQ会将消息路由到备用交换器，而不会返还给Client端，于是回调代码不会被执行。
消息也会被备用交换器路由到它绑定的队列上。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f51cc340a11b4a07a251bb43b672afb7.png)
## 非mandatory
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/eb2c0ad8fa134b11b1250f9482ac2f12.png)
由于存在备用交换器，非mandatory的消息也不会被RabbitMQ抛弃，而是会路由到备用交换器对应的队列中。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f51cc340a11b4a07a251bb43b672afb7.png)
# 总结
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b0d8275bfafb491bb2003c22509ec6af.png)

