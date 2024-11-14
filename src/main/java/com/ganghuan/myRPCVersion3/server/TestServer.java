package com.ganghuan.myRPCVersion3.server;

import com.ganghuan.myRPCVersion3.service.BlogService;
import com.ganghuan.myRPCVersion3.service.BlogServiceImpl;
import com.ganghuan.myRPCVersion3.service.UserService1;
import com.ganghuan.myRPCVersion3.service.UserServiceImpl1;

public class TestServer {
    public static void main(String[] args) {
        UserService1 serverUserService1 = new UserServiceImpl1();
        BlogService blogService = new BlogServiceImpl();

//        Map<String, Object> serviceProvide = new HashMap<>();
//        serviceProvide.put("com.ganghuan.myRPCVersion2.service.UserService1",serverUserService1);
//        serviceProvide.put("com.ganghuan.myRPCVersion2.service.BlogService",blogService);
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(serverUserService1);
        serviceProvider.provideServiceInterface(blogService);

        RPCServer RPCServer = new NettyRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}