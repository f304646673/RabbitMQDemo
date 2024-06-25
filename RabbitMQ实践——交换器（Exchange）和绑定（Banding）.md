@[TOC](大纲)
RabbitMQ在概念上由三部分组成：
- 交换器（Exchange）：负责接收消息发布者发布消息的结构，同时它会根据“绑定关系”（Banding）将消息投递到符合规则的队列中。
- 队列（Queue）：消息索引组成的先进先出（FIFO）结构。交换器会将消息投递到该结构，消费者会消费该结构上的数据。它相当于一个仓储。
- 绑定关系（Banding）:用于定义交换器投递消息到队列的规则。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9ba05cc0dc584321bc82873029dc65fe.png)
本文我们将关注交换器（Exchange）和绑定（Banding）部分，观察它们是如何协作将消息投递到对应队列中。
本文仍然使用[《RabbitMQ实践——在管理后台测试消息收发功能》](https://fangliang.blog.csdn.net/article/details/139655228)中介绍的方法，直接在后台上操作，不涉及任何代码。RabbitMQ已经给我们初始化了各种交换器，我们将基于它们去做测试。
实验之前，我们先建立若干个队列。具体方法如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/684ac7d4636b4f6dadae22b4818925d1.png)

# direct型交换器
direct是最普通和常用的交换器。它可以将一个消息投递到一个或者多个队列，但是它只能使用Banding中的Routing key做完全匹配。也就说，**只有消息中的Routing key和Banding中的Routing key完全一致时，才会将消息投递到Banding对应的队列中**。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7d47e33ec5ca4aefaf4f8fccb00e054a.png)
## 默认交换器
默认交换器是一种direct型交换器。它简化了绑定关系（Banding），即用户不需要自己定义绑定关系（Banding），而是**将队列的名称作为Routing Key使用**。这样只要消息的Routing Key是队列名称，发送到该交换器后，就会被投递到对应的队列上。默认的，**所有队列都会和默认交换器关联**。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4a40083bde3c438fb8951c3203c1ff89.png)

默认交换器位于如下位置
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b9ef983bdeca473c93de0effb105278b.png)
然后使用下面方法发送Routing key为named_queue_a、named_queue_c和named_queue_d的消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/2b7ea51909274e508fdc9e05e6cca4aa.png)
由于named_queue_a、named_queue_c这两个队列存在，所以在发送Routing key为named_queue_a、named_queue_c的消息时，会看到如下提示。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f67e8cbd808841f0bd862d2cb1aeca02.png)
但是named_queue_d队列不存在，所以发送Routing key为named_queue_d的消息会失败。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/e504980bf6564658a7e5f93dd2acdcb6.png)
在队列页面，我们看到named_queue_a、named_queue_c各有一条消息，而named_queue_b则没有，因为我们没有发送Routing key为named_queue_b的消息到默认交换器。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/149d9fdbe0c747099603dd51c9f08c65.png)
## 命名交换器
**命令交换器并不会自动和所有队列关联**，它需要通过绑定关系（Banding）来定义投递规则来将交换器和队列关联起来。
我们使用下面的交换器做测试![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9b684757902c4caa8f6c2b5c32913a16.png)
然后使用下面的步骤新增三个绑定关系（Banding）
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7e60a545663c4001ae9d0db44e1cc87e.png)
最终关系如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d85e91a69cbc48388c1530ef29f9900a.png)
最后发送消息，消息的Routing key是to_b。
可以在队列表中看到消息被投递到named_queue_b队列上。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ec7862bbec684e7fb09a239e3da2721c.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b6c448e3b847456b840a700596bac872.png)

# fanout型交换器
fanout型交换器是一种扇出型交换器。它将消息投递到所有和它绑定的队列上。
这次我们使用的是下图的交换器
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/544b5f1250074998bef78ab83885cfb4.png)
然后配置Banding关系。这次我们**只要指定队列名称，而不用指定Routing key**。（即使指定了Routing Key，也不会生效）
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0c188c9cf2af41fb9df388f964ea0094.png)
绑定完三个队列后
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/c2cec5e5fdb249d3b4580b6c746a631a.png)
然后发布一条消息，只用指定内容，不用指定Routing key。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/59879dc828ef479d9a32f5919b996edf.png)
可以看到这条消息被发布到三个队列里了。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d2214f48f10d4a819c5ed2a88d521244.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/daac31ea435f46a2bf111ab2fc596f29.png)
# topic型交换器
之前介绍的direct型交换器要求Routing key完全相同才能投递，这明显限制了灵活性。而**Topic型交换器则支持Routing key的简单正则匹配**。之所以成为“简单”，是因为它只支持*和#通配符。
Topic型交换器的Routing key要组织形式是：通过.分割的字符串，比如a.b.c。
*表示通配所有.号之前的内容；#表示通配所有字符（包括后续的.）。
举一些例子：
-  a.*.*：可以匹配a.b.c，但是不能匹配a.b.c.d。这是因为a.*.*结构要求只有3段，而a.b.c.d有4段。
- a.#：可以匹配a.b.c和a.b.c.d，但是不能匹配b.c.d.a。
- #.c：可以匹配a.c、a.b.c，但是不能匹配a.b.c.d、c.d.e。

这次我们使用下面的转换器
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6e9fd2a9134f43e7ad77b027d10c44be.png)

如下配置出下面的Banding关系，即只要Routing key中某段（两.之间，或者开头和结尾）是a，则投递到队列named_queue_a。以此类推named_queue_b和named_queue_c。
如果我们发送的Routing key是a.b.c的消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/5b4a0318edc94e9a9cfba48a9457f607.png)
则会按照Banding规则，投递到对应的队列上。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/82da6e4cbd73459fb369e6a5ade59af5.png)
这儿需要注意一点：如果通配规则有重叠，即消息的Routing key匹配了多个通配规则，而这些通配规则都是投递到1个队列上，则消息只会被投递到该队列上1次，而不是匹配的次数。
比如Routing key是a.a.a的消息，它匹配了a.#、#.a.#和#.a这些规则。这些规则都指向了named_queue_a。这时这条消息只会被投递到队列named_queue_a上一次。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/0574ac25e9a04a4984ecf38a5ed3860f.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7b13bf94f91d4f9ea37b94e148b7e907.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f8008f2388434518a0d098573b049a25.png)

# headers型交换器
Header型交换器不像direct和topic型需要匹配Routing key，而是**匹配Banding中的Argument和Message中的Header**。它有两种模式：
- 完全相同：Message中的Header保存的键值对**都在**Banding中的Argument的键值对中，则匹配上。**需要Banding的Argument设置x-match=all**。
- 部分匹配：Message中的Header保存的键值对**部分在**Banding中的Argument的键值对中，则匹配上。**需要Banding的Argument设置x-match=any**。

这次我们使用下图的交换器
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/e9e4d3c4d0084b2482ac99a4ba7df9d6.png)
然后配置如下的Banding关系
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1fe5094468494c39bfc67ba22996ba8b.png)
如果消息Header中有from=a和to=b的，会被投递到named_queue_b。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1d6ef7ee7ce9423799d052dcab5ced17.png)

如果消息Header中有from=a或者to=c的，会被投递到named_queue_c。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/34d63c5e494442e5ba6417f05056aea5.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ade71e084bf54e09b09a0c30964a615e.png)



