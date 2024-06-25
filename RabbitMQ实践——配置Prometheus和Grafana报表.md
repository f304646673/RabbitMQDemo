@[TOC](大纲)

在[《RabbitMQ实践——在Ubuntu上安装并启用管理后台》](https://blog.csdn.net/breaksoftware/article/details/139639464)中我们已经安装成功RabbitMQ及其管理后台。在此基础上，我们将打通它和Prometheus、Grafana的通信，完成对RabbitMQ的可视化监控。
# 启用rabbitmq_prometheus插件
在RabbitMQ所在的机器上执行下面指令

```bash
sudo -H -u rabbitmq bash -c "/usr/lib/rabbitmq/bin/rabbitmq-plugins enable rabbitmq_prometheus"
```
然后在管理后台，我们可以看到数据获取服务已经开启
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f3f28172b2b04cd1975d9c15dee231d5.png)
记录下这个端口号，会在后续配置Prometheus时使用。

# 安装启动Prometheus
我们新开一台机器，同样给2核4G配置，更更新好系统和apt库。
##  创建用户
```bash
sudo groupadd --system prometheus
sudo useradd -s /sbin/nologin --system -g prometheus prometheus
```
## 下载并解压

```bash
wget https://github.com/prometheus/prometheus/releases/download/v2.52.0/prometheus-2.52.0.linux-amd64.tar.gz
tar xvf prometheus-2.52.0.linux-amd64.tar.gz
```
## 修改配置

```bash
cd prometheus-2.52.0.linux-amd64
vim prometheus.yml
```
新增如下内容（172.29.126.58是Rabbit所在的机器IP）

```yaml
  - job_name: rabbitmq_exporter
    static_configs:
    - targets: ['172.29.126.58:15692']
```
## 启动

```bash
mkdir tsdb 
./prometheus --config.file ./prometheus.yml --storage.tsdb.path=./tsdb --web.listen-address="0.0.0.0:9090"  
```

打开prometheus 后台，在Status-Target下可以看到数据已经接收到。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/4921c37a786e448d854995de13aa88db.png)

# 安装启动grafana
## 安装
```bash
cd ~
sudo apt-get install -y adduser libfontconfig1 musl
wget https://dl.grafana.com/enterprise/release/grafana-enterprise_11.0.0_amd64.deb
sudo dpkg -i grafana-enterprise_11.0.0_amd64.deb
```

## 启动
```bash
sudo /bin/systemctl start grafana-server
```

## 配置数据源
登录http://172.29.124.95:3000/login，使用用户名和密码都是admin登录grafana。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/286a0f9b93e04144b10e06ccb658fb3f.png)
数据源选择Prometheus，地址填入刚启动的Promethous服务地址。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/3695e3ae81cc4ff2b0490c51f26521c2.png)
保存后，使用https://grafana.com/grafana/dashboards/10991-rabbitmq-overview/展现数据
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/62c21a7522d245ca808629f14a42c9f1.png)

