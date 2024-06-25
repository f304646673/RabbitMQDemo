在[《RabbitMQ实践——利用一致性Hash交换器做负载均衡》](https://blog.csdn.net/breaksoftware/article/details/139666730)一文中，我们介绍了如何开启一致性hash交换器，并实现了消息的负载均衡，以达到横向扩展消费者数量的能力。
但是现实中，可能存在这样的场景：一些队列所在的机器配置较好，一些队列所在的机器配置较差。这个时候，我们希望配置较好的机器可以分发更多的消息，而配置较差的队列分发相对较少的消息。这样我们就需要让转换器，将消息更多的路由到配置好的机器所在的队列上。
一致性Hash交换器可以使用“权重”来完成这样的设计。
现在我们假设队列named_queue_b所在的机器配置较好，希望它承载更多的消息。在[《RabbitMQ实践——利用一致性Hash交换器做负载均衡》](https://blog.csdn.net/breaksoftware/article/details/139666730)中实操的基础上，我们修改了绑定关系（Banding）中路由到named_queue_b队列的权重——9。其他队列权重还是1，这样理论上这些队列将受到较少的消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/dffb6ae7c50841489b2069e4c55cb603.png)
然后我们再使用代码向x.consistent.hash发送10,0000条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/bcb7d8298c3740099712db696c56d7e9.png)
我们在队列页面可以看到named_queue_b被路由了80%消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/04491156b68f428e990f7a3c2cb39295.png)

