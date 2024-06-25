@[TOC](大纲)

在一些业务场景中，我们只需要保存最近的若干条消息，这个时候我们就可以使用“最大长度队列”来满足这个需求。该队列在收到消息后，如果达到长度上限，会将队列头部（最早的）的信息从队列中移除。
在进行实验之前，我们先创建一个交换器direct.max.length，用于接收消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f0c65e9f1ffb44a7abd391e23981772c.png)
# 抛弃消息
## 创建最大长度队列
我们创建一个名字叫queue.max.length的队列，并且配置属性x-max-length的值为3。这就意味着这条队列最大消息个数是3，超过3个消息时，老消息会被移除。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7d8e0a376b6d4db38fe61e434a6560fe.png)
## 绑定
我们定义Routing key是to.queue.max.length时，向上述创建的队列queue.max.length发布消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4865a7a1800e477dac3fc47a3ca76985.png)
## 实验
我们依次发送消息内容是：1、2、3、4的消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1fad332a0d484db09945e145f2bd04cf.jpeg)
但是该队列最多只保存了3条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/da7b2bfbfd6f4893b783e968d4641b95.png)
最早的一条消息1被抛弃掉了
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/442c9f1ce46a497887bc8fd67ced312e.png)
# 转存死信
如果我们不希望消息被抛弃，则可以将其转存到死信中。
转存到死信有两个方案，在[《RabbitMQ实践——使用死信机制对异常消息进行处理》](https://fangliang.blog.csdn.net/article/details/139709498)中已经有探讨：
- 使用独立的死信交换器
- 重写Routing key

本文我们将使用相对简单的“重写Routing key”方案，这样可以让我们少创建一个交换器。
## 创建死信队列
我们创建一个用于保存死信的队列queue.dead.letter。没做什么特殊配置。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/576e0225060f4219a7bfa419cfd61f1c.png)
## 创建可重写Routing key的最大长度队列
我们创建一个新的队列queue.max.length.with.dead.letter.routing.key。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b33f3f9585bd44f9aef98b14309cea1b.png)
它制定了下面几个属性
* x-max-length：队列最多保存3条消息。
* x-dead-letter-exchange：使用direct.max.length交换器进行消息路由。这个交换器也是原始消息的交换器。
* x-dead-letter-routing-key：修改从队列顶部移除的消息的Routing key为to.queue.dead.letter，以供交换器路由。

## 创建绑定关系
在之前案例创建的交换器direct.max.length上，我们将上述队列绑定。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/75b69744b7394218af1046d6f5499196.png)
## 实验
我们使用Routing key：to.queue.max.length.with.dead.letter.routing.key向queue.max.length.with.dead.letter.routing.key队列发送了4条消息，最终它只保存了后3条，最早的一条会被转存到死信队列中。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ce401ba9a174451ebba9c59790563bd1.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ed41568049e44214b5a0f00d5cb3efe1.png)



