@[TOC](大纲)

在[《RabbitMQ实践——利用一致性Hash交换器做负载均衡》](https://fangliang.blog.csdn.net/article/details/139666730)中，我们使用了Consistent Hash Exchange实践了消息路由的负载均衡。本文我们将使用一种更简单的交换器来实现该功能，这就是“随机交换器”（Random Exchange）。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/e9606bdf5a0e4a999f96a3781001527e.png)

# 启用Random Exchange
和Consistent Hash Exchange一样，Random Exchange默认也是关闭的，所以我们需要通过下面的命令启动该功能：
```bash
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmq-plugins enable rabbitmq_random_exchange"
```
然后我们在创建Exchange页面时，就可以看到该类型。
# 创建Exchange
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/5d955c9eb4a84a1aa764ffc1fdcaf9d3.png)
#  绑定队列
和Consistent Hash Exchange不同的是，给Random Exchange创建Banding时不需要指定Routing key。这就意味着它，它只有单纯的“负载均衡”的功能，而不像Consistent Hash Exchange，可以通过Routing key值表示权重，来“不均衡”路由消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6a911d4bb43d4d2eb2ca695f623dc28f.png)
# 测试

```java
public boolean sendToXCRandomExchange(String exchangeName, Long count) {
    for (int i = 0; i < count; i++) {
        rabbitTemplate.convertAndSend(exchangeName, "", "Message " + i);
    }
    return true;
}
```
然后发送10,0000条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f3fc7f03957e435597a7041378c9ed3f.png)
可以看到这些消息被相对均衡的分配
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f24d2a8135a04c518feaee661cac4153.png)





