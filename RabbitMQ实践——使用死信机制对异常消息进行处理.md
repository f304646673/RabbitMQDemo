@[TOC](大纲)

在[《RabbitMQ实践——对不可路由的消息的处理方法》](https://fangliang.blog.csdn.net/article/details/139694132)一文中，我们使用备用交换器来处理当前交换器不可路由的消息。这类消息的异常往往出现在Routing key这类会影响路由的字段上，而如果消息体自身有问题——结构不符合业务约定、某个字段值不合法，则怎么处理呢？是抛弃还是让其继续保留在队列中？上述两种方式都不太好：如果抛弃则会让我们丧失分析异常的机会；如果保留在队列中则会堆积，影响后续处理效率。
RabbitMQ提供了死信机制来辅助我们处理这类消息。其基本原理就是在业务逻辑判断这些消息存在异常的情况下，让这些消息路由到另外一个独立的队列中。这样后续维护人员可以从这个队列中获取数据进行分析，而不会影响线上业务。
RabbitMQ提供了两个死信机制。
在做这次实验之前，我们先创建一个普通队列dead_letter_queue，用于保存死信。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b08a1322e1bb4384ad0536383850be9f.png)

# 独立的死信交换器
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/87e2ba3840854fb3a7123eda25881e86.png)
如上图，消费者在从正常队列中获取消息。突然发现一个异常消息，它就拒绝该消息，并让RabbitMQ服务不要再发送这条消息。这样RabbitMQ服务会将这条退回到队列中的消息路由到一个独立的死信交换器上，交由它来路由到死信队列。
## 创建死信交换器
为了让死信交换器可以接收所有消息，我们使用了不用做规则匹配的fanout类型交换器。从下图可以看出，死信交换器（我们给它命名dead.letter）和其他交换器没什么区别。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6564745faaa04c1e915f77b517680d80.png)
## 死信交换器绑定队列
我们让这个交换器绑定到之前创建的死信队列dead_letter_queue上。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/7ab9efa4b4144bff922ffeabfb4fd102.png)
## 创建自动路由到死信交换器上的队列
我们创建一个名字叫queue_with_dead_letter_exchange，x-dead-letter-exchange参数值是之前创建的死信交换器dead.letter的队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d57ae07680014c5a84f0f811ba8ef9c8.png)
## 实验
在默认交换器中，我们发布一条消息到queue_with_dead_letter_exchange队列上。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/bc42b6abd6b141709a3806a1eb9cfee8.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b4a55780c59a4ec48cfb77bcca4f2857.png)
然后在该队列页面中，接收这条消息，但是告诉RabbitMQ服务，消费者拒绝这条消息，同时不要重发。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/709255be7a3046c58b6d6a02291c4ccf.png)
这个时候queue_with_dead_letter_exchange队列中就不存在这条消息了。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4ea26a495d974095852d0c643afc7081.png)
它被路由到这条队列（queue_with_dead_letter_exchange）通过x-dead-letter-exchange参数指定的死信交换器dead.letter上。然后死信交换器将其路由到死信队列dead_letter_queue上。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4da7f4961524453bbc2ab135aed9a41a.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1967bdd5db74443c84701099f96afe7b.png)
# 重用原始交换器
之前的案例，我们需要新建一个死信交换器、绑定关系、死信队列才能完成死信所有功能。
而RabbitMQ还提供了一种重用原始交换器的功能，我们只要新建绑定关系、死信队列就可以完成所有功能。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/04fb2e5e50e64303af5b8073886fba52.png)
## 简单案例
因为之前使用的是默认交换器。它并不需要设定绑定关系，而是自动通过各个队列名作为Routing key，将消息路由到队列上。这样在简单案例中，我们省去建立绑定关系这个步骤。
但是我们还是要新建一个队列，它需要将x-dead-letter-exchange设定为消息发送来源的交换器，还要将x-dead-letter-routing-key设定为重写消息的Routing key（指向死信队列），让消息在被消费者拒绝且不再重发后，通过这条消息来源的交换器，再路由到死信队列。
因为我们使用的是默认交换器，所以下图中x-dead-letter-exchange是空值，x-dead-letter-routing-key是死信队列的名称。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/eb8a019ec4e746498e0016617244605f.png)
### 测试
在默认交换器中，我们给刚创建的队列发布一条消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/21b8b1c841614c6da95948139f3f2e36.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/e29b0e022abf4b20aa8e442a54277890.png)
然后消费者拒绝这条消息，并让RabbitMQ不再重发。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/1480349950484534bcdf8027cbc0b601.png)
这条消息就被路由到死信队列中了
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8e20d377447644ec9d1daa181bb8d059.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b55860bb9321433eb348712fcf61520d.png)
## 复杂案例
因为之前我们直接使用了默认交换器，省去了Banding关系这个环节。
本例我们将使用一个稍微常见的组织形式。
### 新建交换器
我们创建一个名字叫direct.with.dead.letter的交换器。该交换器很普通，就是一个direct类型的交换器。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6ef62395c29c4a059aad285c71bfa32c.png)
### 新建队列
新建一个名字叫common_queue的队列。指定x-dead-letter-exchange是上一步创建的交换器名direct.with.dead.letter;指定被拒绝切不重发的消息的Routing key被重写为to.dead.letter。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/94f214e5316544fc985f336a5fb4ae1f.png)
死信队列就沿用之前创建的队列dead_letter_queue。
### 绑定
新建两个绑定关系：
- to.common的Routing key被路由到队列common_queue。
- to.dead.letter的Routing key被路由到队列dead_letter_queue。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ac8856d3ec274924b7b737aff36c3484.png)
### 测试
我们先给common_queue发布一条消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/c6c01a9ce95049c4a4850c88314acd11.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/fd36dcbfc1684f2b99268121891d6aad.png)
然后消费者拒绝这条消息，并通知RabbitMQ不再重发。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ffb0bf2235b24530a53fdd6eda768659.png)
这样，这条消息的Routing key就被重写为to.dead.letter，然后被交换器路由到死信队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/76f696d720de48ccb014bcffb3fb26ad.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/efe6dbfd2dd64b3bbedf385f239089fa.png)


