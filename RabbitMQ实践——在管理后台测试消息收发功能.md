在[《RabbitMQ实践——在Ubuntu上安装并启用管理后台》](https://fangliang.blog.csdn.net/article/details/139639464)中，我们搭建完RabbitMQ服务以及管理后台。本文我们将管理后台，进行一次简单的消息收发实验。
# 赋予admin账户权限
登录到管理后台，进入到用户admin的管理页面
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/12ab9eba47de4716ba31e3741de4bfb2.png)
点击“set permission”，给admin账户授予权限。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f6679b654732416091342e725ec60922.png)
# 创建Queue
进入“Queues and Streams”页面，创建一个类型是Classic,Durablility是Transient(内存型），名称是classic_queue的队列。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/fe9855efc6244af5a8bc00439963be98.png)
# 转发器和绑定关系
RabbitMQ有一个默认的转发器（Exchange）。它会绑定到每个队列，且通过Routing Key判断将消息分发到哪个队列。
所以在本次测试中，我们不需要创建新的转发器和绑定关系。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/09b7435d6ebd4fd49770c7724c2179df.png)
这个默认的转发器是下图中第一个
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/da52f553fafd4f8da91e10a6b8e92f25.png)
我们在刚才创建的队列详细信息中可以看到它们的绑定关系
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/436fe14c6c404739960a82c0dd891ccc.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d38dfa62b96f496ba3c9c78ff92fd3ce.png)
# 测试收发

我们进入默认转发器内部，发布一条消息，内容是test，Routing Key是之前创建的队列名称classic_queue。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/37be50207bb54fe7b9ea22b63850bb16.png)
然后我们可以在队列中看到这条消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/43d53d0963824fbd9348b5843205d73e.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/97b7d10dc35b425cae5eb561ab000d87.png)
然后我们在队列的Get messages功能中就可以获取这样的消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/d836bac64a4041c7a0b7f9ab7591b184.png)


