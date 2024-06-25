@[TOC](大纲)

在[《RabbitMQ实践——利用一致性Hash交换器做负载均衡》](https://fangliang.blog.csdn.net/article/details/139666730)一文中，我们熟悉了一致性Hash交换器的使用方法。默认的，它使用Routing key来做Hash的判断源。但是有些时候，Routing key会有其他作用。比如在Exchange绑定Exchange时，部分Exchange就对Routing key有很强的限制。这种时候，我们就可以通过自定义路由字段来避开对Routing key的依赖。
# Property法
Property法是指利用消息的Property中的一些字段来作为路由字段。
这相当于要定义交换器的判断标准，所以我们在创建交换器时，需要做特殊处理。
## 定制交换器
我们创建一个名字叫x.consistent.hash.from.property的一致性hash交换器。并且要在Arguments中**设置hash-property字段**（固定），它的值是property中的某个字段，比如message_id。通过hash-header的值，我们告诉一致性hash交换器，要使用消息Header中的message_id字段做消息路由判断源。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0298988e0a51425991d6aea08db06a4f.png)
## 绑定队列
和[《RabbitMQ实践——利用一致性Hash交换器做负载均衡》](https://fangliang.blog.csdn.net/article/details/139666730)介绍的案例一样，我们使用负载均衡的方式绑定到3个队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/192a00b965464c01a06e66e88ab76209.png)
## 测试
我们将hash值设置到消息message_id中，然后发布给交换器。注意convertAndSend的Routing key参数可以传空串，因为后续也用不到。

```java
    public boolean sendToXConsistentHashExchangeWithMessageId(String exchangeName, Long count) {
        for (int i = 0; i < count; i++) {
            int hash = Integer.hashCode(i);
            Message message = MessageBuilder.withBody(("Message " + i).getBytes())
                .setMessageId(String.valueOf(hash))
                .build();
            rabbitTemplate.convertAndSend(exchangeName, "",  message);
        }
        return true;
    }
```
发布10,0000条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7a48167d62f743d1abd02ba3ee928ee1.png)
可以看到消息被相对平均的路由到各个队列上
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/a9d8fcb564914a9b88b2c801d7454356.png)


# Header法
Header法是指利用消息Header中的自定义字段来实现。它比Property法更加的自由。
## 定制交换器
我们创建一个名字叫x.consistent.hash.from.header的一致性hash交换器。并且要在Arguments中**设置hash-header字段**（固定），它的值是hash（自定义）。通过hash-header的值，我们告诉一致性hash交换器，要使用消息Header中的hash字段做消息路由判断源。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8baa0a938db7496e977cdeab1403cbc9.png)
## 绑定队列
和[《RabbitMQ实践——利用一致性Hash交换器做负载均衡》](https://fangliang.blog.csdn.net/article/details/139666730)介绍的案例一样，我们使用负载均衡的方式绑定到3个队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/192a00b965464c01a06e66e88ab76209.png)
## 测试
我们将hash值设置到消息Header中，然后发布给交换器。注意convertAndSend的Routing key参数可以传空串，因为后续也用不到。
```java
public boolean sendToXConsistentHashExchangeWithHeader(String exchangeName, Long count) {
    for (int i = 0; i < count; i++) {
        int hash = Integer.hashCode(i);
        Message message = MessageBuilder.withBody(("Message " + i).getBytes())
            .setHeader("hash", hash)
            .build();
        rabbitTemplate.convertAndSend(exchangeName, "",  message);
    }
    return true;
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/2e3a4e005b7c4c019490e5ee241499e8.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d9bd7f5473a04f4c8f90a5adc014cf22.png)



# 参考资料
- [https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_consistent_hash_exchange](https://github.com/rabbitmq/rabbitmq-server/tree/main/deps/rabbitmq_consistent_hash_exchange)
