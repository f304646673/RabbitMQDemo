@[TOC](大纲)
在之前的章节中，我们基本都是在管理后台创建“队列”（Queue）、“交换器”（Exchange）和“绑定关系”（Banding）。实际上，我们还可以在代码中完成这些功能。这些行为我们可以通过RabbitMQ提供的“事件交换器”（Event Exchange）来监控这些行为。
事件交换器（Event Exchange）是一个Topic类型的交换器。向它发送消息的是RabbitMQ服务。我们可以定义一个队列来接收这些消息，然后定义一个消费者来读取该队列，完成监控功能。
# 启用Event Exchange
Event Exchange默认是关闭的，我们可以通过下面的命令开启这个交换器
```bash
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmq-plugins enable rabbitmq_event_exchange"
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7a48df3f395243e49161b8ab04b747d8.png)
# 创建队列
创建名字叫rabbitmq_events的队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/21ca546ae273436f8eea8a12f3cbce37.png)
# 绑定队列和Event Exchange
由于Event Exchange是Topic类型交换器，所以Routing key要符合Topic风格。
Event消息的Routing Key可以是下面值：

| 值 | 说明      |
|:--------:|:-------------:|
| centered 文本居中 | right-aligned 文本居右 |
| queue.deleted| 删除队列| 
| queue.created| 创建队列| 
| exchange.created| 创建交换器| 
| exchange.deleted| 删除交换器|
| binding.created| 创建绑定关系|
| binding.deleted| 删除绑定关系|
| connection.created| 创建连接|
| connection.closed| 关闭连接|
| channel.created| 创建通道|
| channel.closed| 关闭通道|
| consumer.created| 创建消费者|
| consumer.deleted| 删除消费者|
| ……|更多见[https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_event_exchange](https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_event_exchange)|

我们设置成#.#，表示接收所有消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/717cf282779448ccb81b9e36bf762fdb.png)
# 测试
我们创建一个新的队列
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d516d9816fec463fa8ee65aa456ed740.png)
然后在之前创建的rabbitmq_events中，可以找到刚创建队列产生的事件信息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/86645b0a707d41ce8c1c952958859fe3.png)


# 参考资料
-  [https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_event_exchange](https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_event_exchange)
