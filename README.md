# Pangolin
内网穿透工具 基于JDK8,netty4.x实现

#### How to user

    1. 配置server模块下的user.json文件,配置公网访问端口,对应的私钥privateKey
    2. mvn clean package  
    3. java -jar pangolin_server.jar 7500  
    4. java -jar pangolin_client.jar -p_host `xxxx` -p_port 7500 -r_host `yyyy` -r_port `iiii` -p_key `zzzz`

#### Args description  
    p_host 代理服务器主机名称  
    p_port 代理服务器端口  
    r_host 被代理机器主机名称  
    r_port 被代理机器端口  
    p_key 客户端分配的私钥


#### Manage api  
    查询所有在线通道信息  
    - http://127.0.0.1:7060/channel/online/info  

    查询公网端口配置信息  
    - http://127.0.0.1:7060/public/port/conf  

    关闭已失活的通道  
    - http://127.0.0.1:7060/channel/inactive/close
