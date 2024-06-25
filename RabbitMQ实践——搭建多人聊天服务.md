@[TOC](大纲)

在[《RabbitMQ实践——搭建单人聊天服务》](https://fangliang.blog.csdn.net/article/details/139757261)一文中，我们搭建了Tom和Jerry两人的聊天服务。在这个服务中，它们都向Fanout交换器发送消息。而Fanout会将消息路由到它们两各自监听的队列。这样它们就可以得到全部消息。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ff208d88857e46eb89d1eef4cac929d4.png#pic_center)
如果是多人聊天，比如10个人聊天，按上述方案，需要Fanout交换器绑定10个队列。这就会使得结构变得非常复杂。
这是因为Classic类型队列在消费者确认读取消息后，会将消息从队列中删除。这样就需要我们使用fanout向多个队列路由消息，以供不同消费者消费。如果多个消费者消费同一个队列，则会导致每个消费者得到的都是部分信息。这就不符合我们理解的聊天场景。
但是我们可以使用Stream类型队列来解决这个问题。
Stream类型队列和之前的Classic队列的不同点是：Stream队列并不会清除消息。消息会一直存在于Stream队列中，消费者可以从指定位置开始读取消息。这样我们只要有一个Stream队列保存消息，所有消费者都从队列中读取消息即可。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/aa872b1836534d7cbda7cb8a9982b418.png)

# 用户登录
关于用户登录的流程我们在[《RabbitMQ实践——搭建单人聊天服务》](https://fangliang.blog.csdn.net/article/details/139757261)中已经有详细的介绍。即上图中黑色字体1、2、3、4、5的步骤。
# 创建聊天室
我们会创建一个以聊天室名称命名的交换器和Stream类型队列。即上图中黑色字体6、7、8、9的步骤。
需要注意的是Stream类型队列创建方案和Classic类型类似，只需要多指定"x-queue-type"="stream"。但是对于Durable（持久化）只能设置为True，exclusive只能设置为False，autoDelete只能设置为False。
```java
package com.rabbitmq.chat.service;

import java.util.Collections;
import java.util.Date;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatRoomV2 {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void createChatRoom(String admin, String roomName) {
        createChatRoom(roomName);
    }

    private void createChatRoom(String roomName) {
        rabbitTemplate.execute(action -> {
            action.exchangeDeclare(roomName, "fanout", false, true, null);
            action.queueDeclare(roomName, true, false, false,
                Collections.singletonMap("x-queue-type", "stream"));
            action.queueBind(roomName, roomName, "");
            return null;
        });
    }
```
聊天室创建完毕后，会通知所有登录的用户。

```java
    @PostMapping("/create")
    public void create(@RequestParam String admin, @RequestParam String roomName) {
        chatRoomV2.createChatRoom(admin, roomName);
        core.notifyEveryone(roomName + " is created");
    }
```
# 监听Stream（聊天室）
```java
    public Flux<String> receive(String username, String roomName) {
        return Flux.create(emitter -> {
            rabbitTemplate.execute(channel -> {
                channel.basicQos(100);
                Date timestamp = new Date(System.currentTimeMillis());
                channel.basicConsume(roomName, false, username,
                    false, true,
                        Collections.singletonMap("x-stream-offset", timestamp),
                        (consumerTag, message) -> {
                            String senderOfMessage = message.getProperties().getHeaders().get("username").toString();
                            String show = "You Said: ";
                            if (!senderOfMessage.equals(username)) {
                                show = senderOfMessage + " Said: ";
                            }
                            show += new String(message.getBody());
                            System.out.println(show);
                            emitter.next(show);
                            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                        },
                        consumerTag -> { }
                );
                return null;
            });
        });
    }
```
我们将"x-stream-offset"设置为当前毫秒数，是表示我们只读取当前时间之后发布的消息。这也符合聊天室的业务特点：不能读取历史消息。
当我们收到消息后，我们会获取消息Header中的自定义字段username，它标志了消息的发布者。如果发布者和读取者是同一人，我们将展示内容前面新增“You Said:”；如果是别人说的，则标记发布者的名称。
由于我们使用了WebFlux响应式编程，所以Controller层要做特殊处理

```java
    @GetMapping(value = "/receive", produces = "text/event-stream")
    public Flux<String> receive(@RequestParam String username, @RequestParam String roomName) {
        return chatRoomV2.receive(username, roomName);
    }
```

# 发送消息
每个聊天室用户只要给之前创建的Fanout交换器发送消息即可。在这一步，我们给他们发送的消息Header中新增了字段username，以标记是谁发送的。
```java
    public void send(String username, String roomName, String message) {
        Message msg = MessageBuilder.withBody(message.getBytes())
            .setHeader("username", username)
            .build();
        rabbitTemplate.send(roomName, "", msg);
    }
```
# 实验
## 登录
### Tom侧
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/6d20ead93d59404eb0cb5a58315c3a86.png)
### Jerry侧
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/b225f8b3332c46d698dc24756e1b0e70.png)
## 创建聊天室
### Jerry侧
Jerry申请创建一个聊天室
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ba5b9673f6454fa1a2d5ef68c16e96f9.png)
在管理后台，我们看到对应的交换器和Stream都创建出来了。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8967bf7596114290b71bc4088b83c560.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/993f1341af274089b5eb9bf241093ec0.png)
同时在刚才的登录接口界面，Jerry收到了通知
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/24c6bc5fac6e41c1984875136d8a35f9.png)
### Tom侧
Tom也会收到通知
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/c1822784819a4658af64842f70be2839.png)
## 进入聊天室
Tom和Jerry在收到通知后，可以通过receive接口进入聊天室，监听聊天室内容变化。
### Jerry侧
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/bb6a057ee88644a394294e8d5dce2ea1.png)
### Tom侧
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/cd7ba7c6d66b4504a80e7aebe593e342.png)
## 发送消息
### Jerry发送消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/69c4d050c44444d2bb18319293ac087a.png)
#### Jerry侧聊天室
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8ba920aad7c04600b914c7b54951aadc.png)
#### Tom侧聊天室
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/658dcb9b73ef4b578f4e60920a263b03.png)
### Tom发送消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9b0d8d3c361d4cd68c7eac7561b1dbc0.png)

#### Jerry侧聊天室
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/28cae304dc2140148692d83966257831.png)
#### Tom侧聊天室
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/5b67bfd793724851a3486ce56782e647.png)
# 参考资料
* [https://www.rabbitmq.com/docs/streams](https://www.rabbitmq.com/docs/streams)
