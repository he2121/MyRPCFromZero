package com.ganghuan.myRPCVersion3.server;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 存放服务接口名与服务端对应的实现类
 * 服务启动时要暴露其相关的实现类
 * 根据request中的interface调用服务端中相关实现类
 */
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个接口，所以这里把服务与接口分开了，
     * 前面这两个概念是混合的
     */
    private Map<String, Object> interfaceProvider;
    private Set<String> services;

    public ServiceProvider(){
        this.interfaceProvider = new HashMap<>();
        this.services = new HashSet<>();
    }

    public void provideServiceInterface(Object service){
        String serviceName = service.getClass().getName();
        if(!services.add(serviceName)) return;
        Class<?>[] interfaces = service.getClass().getInterfaces();

        for(Class clazz : interfaces){
            interfaceProvider.put(clazz.getName(),service);
        }

    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }
}
