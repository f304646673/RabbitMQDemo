@[TOC](大纲)

RabbitMQ是一款功能强大、灵活可靠的消息代理软件，为分布式系统中的通信问题提供了优秀的解决方案。无论是在大规模数据处理、实时分析还是微服务架构中，RabbitMQ都能发挥出色的性能，帮助开发者构建高效、稳定的系统。
本系列我们将基于Java技术栈，探索RabbitMQ的一些使用。
# 环境
操作系统是Ubuntu Server 24.04 LTS

```bash
cat /proc/version
```

> Linux version 6.8.0-35-generic (buildd@lcy02-amd64-020) (x86_64-linux-gnu-gcc-13 (Ubuntu 13.2.0-23ubuntu4) 13.2.0, GNU ld (GNU Binutils for Ubuntu) 2.42) #35-Ubuntu SMP PREEMPT_DYNAMIC Mon May 20 15:51:52 UTC 2024

# 安装
```bash
sudo apt install rabbitmq-server
```
此时安装程序会新增一个名字叫rabbitmq的用户。
# 启动管理后台
下面的指令会用rabbitmq用户身份启动管理后台，并新增一个用户名是admin、密码是admpwd的用户，且给这个用户设置为管理员（administrator）权限。
```bash
service rabbitmq-server stop
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmq-plugins enable rabbitmq_management"
service rabbitmq-server start
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmqctl add_user admin admpwd"
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmqctl set_user_tags admin administrator"
service rabbitmq-server restart
```
这样我们就可以在浏览器中打开管理后台（http://*yourhost*:15672/#/）
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/05396d35b5d84191992b3919d4e1267d.png)




