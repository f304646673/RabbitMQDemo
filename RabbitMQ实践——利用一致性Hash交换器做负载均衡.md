@[TOC](大纲)

在[《RabbitMQ实践——交换器（Exchange）和绑定（Banding）》](https://fangliang.blog.csdn.net/article/details/139663306)中，我们熟悉了Direct、Fanout、Topic和Header这4种系统默认支持的交换器。这些交换器基本可以满足我们日常的需求。我们还可以添加一些设计，让其支持更加丰富的功能。比如我们可以通过Topic的设计，达到“负载均衡”的功能。
如果不想做这样的设计，可以使用RabbitMQ自身携带，只是未开启的一致性Hash交换器，来实现对队列的平均分流。这样我们让更多消费者订阅不同的队列，来增加整理系统的处理能力。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/160ec2f3487846db8f94ad1fe94e3f56.png)
# 开启一致性Hash交换器
在RabbitMQ所在的服务终端上执行：
```bash
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmq-plugins enable rabbitmq_consistent_hash_exchange"
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/76cac32f77cd4ee78b86af17c6fad11d.png)
# 创建交换器
这样我们就可以新建该类型交换器
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/359f571b2e2d4a138b318316198f2b45.png)
# 创建绑定关系
在一致性Hash交换器中，绑定关系的Routing key被当做权重使用。如果数值越大，被路由到对应的队列上的消息也越多。
本例我们测试“负载均衡”的场景，所以把三个队列的路由权重都设置的比较低且一致。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/16d18ed321b44530875fa88572b4f981.png)
# 测试
```java
public boolean sendToXConsistentHashExchange(String exchangeName, Long count) {
    for (int i = 0; i < count; i++) {
        int hash = Integer.hashCode(i);
        rabbitTemplate.convertAndSend(exchangeName, String.valueOf(hash), "Message " + i);
    }
    return true;
}
```
然后发送10,0000条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/c705d2c4111a4f319247a247fdaed33e.png)
可以看到消息被相对均衡的路由到各个队列上
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/923e23c90ee64bfea4ddd2bdd99b84fc.png)

# 参考资料
- [https://github.com/rabbitmq/rabbitmq-consistent-hash-exchange](https://github.com/rabbitmq/rabbitmq-consistent-hash-exchange)
- [https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_consistent_hash_exchange](https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_consistent_hash_exchange)
