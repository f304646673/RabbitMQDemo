@[TOC](大纲)

在项目中，我们往往会遇到消息具有时效性：在一定时间内的消息需要处理，其他消息则不用处理。RabbitMQ提供了两种功能来满足这种需求：
* 整个队列的消息都有相同的时效性。
* 消息可以指定自己的超时时间。

对于超时的消息，我们可以选择抛弃，或者让其进入死信。本文我们将实验这些场景。
# 准备工作
我们先新建一个交换器direct.ttl。后续不同场景我们再设定其不同路由。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/44cdae0a0ac04d9ba2627ca922001056.png)
然后再创建一个死信队列queue.dead.letter
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/413ff986a37f45a5810e1d78ff4ee3f5.png)
最后新建交换器direct.ttl和队列queue.dead.letter的路由绑定关系
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8ebd9c72bc89417cabf7398ab7e0b681.png)

后续我们将根据不同场景新建不同的队列和绑定关系。
# 整个队列的消息都有相同的时效性
## 抛弃超时消息
### 新建带x-message-ttl的队列
如果队列设置了x-message-ttl，则其全部消息的最大ttl就是其值。它的单位是毫秒。下图中，队列消息如果30秒没有处理，就会从队列中移除。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/74325c4fe7b5427db11ea2e22e177b06.png)
### 新建绑定关系
我们让交换器中Routing key是to.queue.all.message.ttl的消息路由到上面创建的队列中。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d7e90f61cb264ad3ad7c593def01143b.png)
### 实验
向该交换器发送一条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/5790587452c949029865bb262fbadf54.png)
消息出现在队列queue.all.message.ttl中。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/db1795042c254f6ab69419fd391a6a46.png)
半分钟后，该条消息从队列中消失。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/2c3c06a660824befadc82979189b33ac.png)
## 超时消息路由到死信队列
不同于上例，只是设置了x-message-ttl，还要设置死信相关参数。本例我们将使用“重写Routing key”的方案，节省一次交换器的创建。
### 新建带死信和ttl的队列
新建名字是queue.all.msg.ttl.dead.letter的队列。这个队列中的所有消息的最大超时时间（x-message-ttl）是30秒；超时消息会被修改Routing key为to.queue.dead.letter后，使用direct.ttl交换器来路由。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/083729acf2ed4dbd8d03423c1dc95c9c.png)
### 新建绑定关系
将这个队列和之前新建的交换器direct.ttl关联。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/2e16e53f11944b5e81726b696c22e7a1.png)
### 实验
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9903fa4303384d24b11d728843a1a879.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ab84588661d446b5aa8bc3b8a82e52bd.png)
可以看到消息在过了30秒后，被从原来的队列queue.all.msg.ttl.dead.letter中移除，然后路由到死信队列queue.dead.letter中。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/147312f5edf4483a82e6aa1b18e86810.png)
# 消息指定自己的超时时间
本例我们只实验相对复杂的场景，即将超时消息路由到死信。
## 新建带死信的队列
这个队列我们没有设置ttl相关数据，只是设置了死信相关配置：死信消息交由交换器direct.ttl处理，其Routing key被修改成to.queue.dead.letter。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/050594b8eeaf4186871e7ad6a6ac77f1.png)
## 绑定
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/56036091c0d84858a3fb11514fe12150.png)
## 实验
这次我们不能使用后台来发送消息，而是通过Java代码来发送。
```java
public void sendWithTTL(String exchangeName, String routingKey, String message, int ttl) {
    String msgId = UUID.randomUUID().toString();
    Message msg = MessageBuilder.withBody(message.getBytes())
            .setContentType("text/plain")
            .setCorrelationId(msgId)
            .setMessageId(msgId)
            .setExpiration(String.valueOf(ttl))
            .build();

    CorrelationData correlationData = new CorrelationData(msgId);
    rabbitTemplateWithoutMandatory.convertAndSend(exchangeName, routingKey, msg, correlationData);
}
```
发送消息时我们只需要给消息增加setExpiration调用即可。它的单位是毫秒，需要转换为字符串。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4a2b6a765b5240298a1372440d8fb996.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/88b78b1cc52643fd8b79545ba15ad80a.png)
可以看到20秒后，消息从queue.with.dead.letter队列进入queue.dead.letter队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f7bf5be6916e44259fe5ed54c7b9a54f.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/65a6509316ce4abcbaf85752ece01fda.png)

# 消息自带TTL和队列TTL的关系
如果一个自带TTL的消息被路由到一个所有消息都被队列指定TTL的队列上，那么哪个TTL生效呢？
我们用第二个案例的接口，给第一个案例的队列发送一个自带TTL的消息。
## 消息TTL < 队列指定TTL
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/5c2ebc2bf6ec4d66a9a89233ff32e1df.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/da06cc117eea4554a28a2fc92e4a2d84.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/598138679a724ee09195defef00f231a.png)
可以看到消息在20秒时就进入了死信。

## 消息TTL > 队列指定TTL
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1ee99efa0a0f47ea9728373dc3ac0613.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/418d361b5d5a4637ab42f380a2d34a96.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9afcdae649324fb2b545be2c4675ff16.png)
可以看到消息在30秒时就进入了死信。
## 总结
如果队列指定TTL，消息也设置了TTL，取最接近当前时间的TTL。即用最短的那一个。
