package com.ganghuan.myRPCVersion7.register;

import com.ganghuan.myRPCVersion7.loadbalance.LoadBalance;
import com.ganghuan.myRPCVersion7.loadbalance.RandomLoadBalance;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;

public class ZkServiceRegister implements ServiceRegister {
    // curator 提供的zookeeper客户端
    private CuratorFramework client;

    private PathChildrenCache cache;

    private static HashMap<String, List<String>> serviceMap  = new HashMap<>();


    // zookeeper根路径节点
    private static final String ROOT_PATH = "MyRPC";
    // 初始化负载均衡器， 这里用的是随机， 一般通过构造函数传入
    private LoadBalance loadBalance = new RandomLoadBalance();

    // 这里负责zookeeper客户端的初始化，并与zookeeper服务端建立连接
    public ZkServiceRegister(){
        // 指数时间重试
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        // zookeeper的地址固定，不管是服务提供者还是，消费者都要与之建立连接
        // sessionTimeoutMs 与 zoo.cfg中的tickTime 有关系，
        // zk还会根据minSessionTimeout与maxSessionTimeout两个参数重新调整最后的超时值。默认分别为tickTime 的2倍和20倍
        // 使用心跳监听状态
        this.client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper 连接成功");
    }

    @Override
    public void register(String serviceName, InetSocketAddress serverAddress){
        try {
            // serviceName创建成永久节点，服务提供者下线时，不删服务名，只删地址
            if(client.checkExists().forPath("/" + serviceName) == null){
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/" + serviceName);
            }
            // 路径地址，一个/代表一个节点
            String path = "/" + serviceName +"/"+ getServiceAddress(serverAddress);
            // 临时节点，服务器下线就删除节点
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (Exception e) {
            System.out.println("此服务已存在");
        }
    }
    // 根据服务名返回地址,服务发现
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        // 先从本地内存的缓存中取地址
        List<String> strings = serviceMap.getOrDefault(serviceName, null);
        if(strings != null) {
            String balance = loadBalance.balance(strings);
            return parseAddress(balance);
        }

        try {
            // 缓存没有则去zookeeper取
            strings = client.getChildren().forPath("/" + serviceName);
            // 地址存入缓存
            serviceMap.put(serviceName, strings);
            // 负载均衡选择器，选择一个
            String string = loadBalance.balance(strings);
            return parseAddress(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void createWatch(String path) {
        // 在路径前添加斜杠，以确保路径格式正确
        path = "/" + path;

        // 创建一个 PathChildrenCache 实例，监视指定路径的子节点变化
        // 第一个参数是 CuratorFramework 客户端实例
        // 第二个参数是需要监视的路径
        // 第三个参数指定是否需要递归监视子节点
        cache = new PathChildrenCache(client, path, true);

        try {
            // 启动 PathChildrenCache 实例，使其开始监视指定路径
            cache.start();

            // 添加监听器来处理子节点的变化
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    // 检查事件类型，处理节点更新或删除事件
                    if (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED || event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                        // 获取发生变化的节点路径，并将其按照斜杠分割
                        String[] split = event.getData().getPath().split("/");
                        // split[1]是发生变化的节点服务名称 向zookeeper重新获取该节点服务下的所有地址
                        List<String> strings = client.getChildren().forPath("/" + split[1]);
                        // 更新缓存
                        serviceMap.put(split[1], strings);
                    }
                }
            });
        } catch (Exception e) {
            // 捕获和打印异常信息
            e.printStackTrace();
        }
    }

    // 地址 -> XXX.XXX.XXX.XXX:port 字符串
    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() +
                ":" +
                serverAddress.getPort();
    }
    // 字符串解析为地址
    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }
}
