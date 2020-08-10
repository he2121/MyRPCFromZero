package com.ganghuan.myRPCVersion6.server;



import com.ganghuan.myRPCVersion6.register.ServiceRegister;
import com.ganghuan.myRPCVersion6.register.ZkServiceRegister;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 存放服务接口名与服务端对应的实现类
 * 服务启动时要暴露其相关的实现类
 * 根据request中的interface调用服务端中相关实现类
 *
 * 后面在这里新增了在zookeeper中注册的功能
 */
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个服务接口，
     */
    private Map<String, Object> interfaceProvider;

    private ServiceRegister serviceRegister;
    private String host;
    private int port;

    public ServiceProvider(String host, int port){
        // 需要传入服务端自身的服务的网络地址
        this.host = host;
        this.port = port;
        this.interfaceProvider = new HashMap<>();
        this.serviceRegister = new ZkServiceRegister();
    }

    public void provideServiceInterface(Object service){
        Class<?>[] interfaces = service.getClass().getInterfaces();

        for(Class clazz : interfaces){
            // 本机的映射表
            interfaceProvider.put(clazz.getName(),service);
            // 在注册中心注册服务
            serviceRegister.register(clazz.getName(),new InetSocketAddress(host,port));
        }

    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }
}
