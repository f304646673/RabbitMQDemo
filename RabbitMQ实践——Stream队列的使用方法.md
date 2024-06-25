@[TOC](大纲)
在[《RabbitMQ实践——搭建多人聊天服务》](https://fangliang.blog.csdn.net/article/details/139778946)一文中，我们使用Stream队列存储了聊天室记录。但是每个进入聊天室的人不能读取历史消息，只能读取当前时间之后的消息。这是因为我们对读取逻辑做了特殊设置。本文我们将全面介绍Stream队列的使用。
# 什么是Stream队列
Stream队列保存了发布到其上所有未过期（时间或Size判断）的消息。消费者只可以读取该队列，但是不能让队列将已读消息删除。这样就可以保证相同配置的消费者可以读取到相同的消息。
鉴于它保留了未过期消息，所以非常适合需要读取历史消息的场景。
鉴于消费者不能让其删除已读消息，所以对于需要“扇出”大量相同消息的场景，可以使用一个Stream来替代Fanout交换器绑定多个相同消息队列的方案。这样即可以降低系统设计的复杂度，也会提升Rabbitmq服务效率。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f3ad64aa41454e8caa3f68ef3aa167e4.png)

# 创建Stream
下面代码会创建一个Stream。
```java
action.queueDeclare(roomName, true, false, false,
      Collections.singletonMap("x-queue-type", "stream"));
```
需要注意的是：
* durable（第二个参数）只能设置为true。
* exclusive（第三个参数）只能设置为false
* autoDelete（第四个参数）只能设置为false。
下面完整代码，除了创建了Stream，还创建了交换器以及它们之间的绑定。

```java
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
# 发布消息
发布消息没什么特别，直接给交换器发送消息即可。
```java
rabbitTemplate.send(roomName, "", msg);
```
# 消费
由于Stream中保存了所有未超时的消息，所以存在一个起始读取位置的问题。
还有两个比较特殊的情况需要注意：
* 不可以“自动应答”，即AutoACK只能是false。所以我们要对每条消息手工ack。
* 必须指定Qos。因为消费者需要手工应答，所以需要设置一个配额，这样可以保证过慢的服务减少获取消息，从而让服务分发消息更加合理。

一般常见的模式如下：
## 从第一条消息开始读取
"x-stream-offset"设置为"first"，就是从第一条消息开始读取。
```java
channel.basicQos(100);
channel.basicConsume(roomName, false, username,
      false, true,
          Collections.singletonMap("x-stream-offset", "first"),
          (consumerTag, message) -> {
              // Your code
              channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
          },
          consumerTag -> { }
      );
```

## 从最后一条消息开始读取
"x-stream-offset"设置为"last"，就是从第一条消息开始读取。
```java
channel.basicQos(100);
channel.basicConsume(roomName, false, username,
      false, true,
          Collections.singletonMap("x-stream-offset", "last"),
          (consumerTag, message) -> {
              // Your code
              channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
          },
          consumerTag -> { }
      );
```
## 从某个时间戳开始读取
```java
Date timestamp = new Date(System.currentTimeMillis() - 60 * 60 * 1_000)
channel.basicQos(100);
channel.basicConsume(roomName, false, username,
      false, true,
          Collections.singletonMap("x-stream-offset", "last"),
          (consumerTag, message) -> {
              // Your code
              channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
          },
          consumerTag -> { }
      );
```
## 从某个偏移量开始读取

```java
channel.basicQos(100);
channel.basicConsume(roomName, false, username,
	false, true,
	    Collections.singletonMap("x-stream-offset", offset),
	    (consumerTag, message) -> {
	        emitter.next(new String(message.getBody()));
	        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
	    },
	    consumerTag -> { }
	);
```
# 样例
延续[《RabbitMQ实践——搭建多人聊天服务》](https://fangliang.blog.csdn.net/article/details/139778946)的案例，上面几个场景的读取代码如下：

```java
    public Flux<String> getMessageFromFirst(String username, String roomName) {
        return Flux.create(emitter -> {
            rabbitTemplate.execute((ChannelCallback<Void>) channel -> {
                channel.basicQos(100);
                channel.basicConsume(roomName, false, username,
                false, true,
                    Collections.singletonMap("x-stream-offset", "first"),
                    (consumerTag, message) -> {
                        emitter.next(new String(message.getBody()));
                        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                    },
                    consumerTag -> { }
                );
                return null;
            });
        });
    }

    public Flux<String> getMessageFromLast(String username, String roomName) {
        return Flux.create(emitter -> {
            rabbitTemplate.execute((ChannelCallback<Void>) channel -> {
                channel.basicQos(100);
                channel.basicConsume(roomName, false, username,
                false, true,
                    Collections.singletonMap("x-stream-offset", "last"),
                    (consumerTag, message) -> {
                        emitter.next(new String(message.getBody()));
                        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                    },
                    consumerTag -> { }
                );
                return null;
            });
        });
    }

    public Flux<String> getMessageFromTimestamp(String username, String roomName, Date timestamp) {
        return Flux.create(emitter -> {
            rabbitTemplate.execute((ChannelCallback<Void>) channel -> {
                channel.basicQos(100);
                channel.basicConsume(roomName, false, username,
                false, true,
                    Collections.singletonMap("x-stream-offset", timestamp),
                    (consumerTag, message) -> {
                        emitter.next(new String(message.getBody()));
                        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                    },
                    consumerTag -> { }
                );
                return null;
            });
        });
    }

    public Flux<String> getMessageFromOffset(String username, String roomName, long offset) {
        return Flux.create(emitter -> {
            rabbitTemplate.execute((ChannelCallback<Void>) channel -> {
                channel.basicQos(100);
                channel.basicConsume(roomName, false, username,
                false, true,
                    Collections.singletonMap("x-stream-offset", offset),
                    (consumerTag, message) -> {
                        emitter.next(new String(message.getBody()));
                        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                    },
                    consumerTag -> { }
                );
                return null;
            });
        });
    }
```
# 长度控制
由于Stream并不会因为消费者而删除消息，导致其保存的消息数量一直在增加。所以需要通过一定的策略控制其大小。
## 长度控制
在创建Stream时，我们可以通过x-max-length-bytes设置其最大Size。这样如果Stream内容达到这个Size，最早的消息就会被Stream淘汰掉。

```java
Map<String, Object> args = new HashMap<>();
args.put("x-max-length-bytes", maxSize);
args.put("x-queue-type", "stream");
action.queueDeclare(roomName, true, false, false, args);
```

## 时间控制
在创建Stream时，我们可以通过x-max-age设置消息的最长生命周期。超过这个时长的消息会被淘汰。它的取值可以是如下单位：Y, M, D, h, m, s。比如“1m”表示一分钟。
```java
Map<String, Object> args = new HashMap<>();
args.put("x-max-age", ttl);
args.put("x-queue-type", "stream");
action.queueDeclare(roomName, true, false, false, args);
```
## 服务端筛选消息
如果消费者并不关系Stream中所有消息，它可以通过"x-stream-filter"来做过滤。这个过滤会发生在服务端，这样可以大大减轻消费者和服务端的压力。但是需要注意的是，服务端的过滤使用的是布隆过滤器，所以发送到消费者端的消息会包含不符合条件的消息，所以消费端需要做二次校验才可以使用。
### 发布方设定过滤值

```java
channel.basicPublish(
  "", // default exchange
  "my-stream",
  new AMQP.BasicProperties.Builder()
    .headers(Collections.singletonMap(
      "x-stream-filter-value", "california" // set filter value
    ))
    .build(),
  body
);
```

### 消费方设置服务端过滤，且要二次过滤

```java
channel.basicQos(100); // QoS must be specified
channel.basicConsume(
  "my-stream",
  false,
  Collections.singletonMap("x-stream-filter", "california"), // set filter
  (consumerTag, message) -> {
    Map<String, Object> headers = message.getProperties().getHeaders();
    // there must be some client-side filter logic
    if ("california".equals(headers.get("x-stream-filter-value"))) {
      // message processing
      // ...
    }
    channel.basicAck(message.getEnvelope().getDeliveryTag(), false); // ack is required
  },
  consumerTag -> { });
```

# 工程代码
[https://github.com/f304646673/RabbitMQDemo/tree/main/chat](https://github.com/f304646673/RabbitMQDemo/tree/main/chat)

# 参考资料
* [https://www.rabbitmq.com/docs/streams](https://www.rabbitmq.com/docs/streams)
