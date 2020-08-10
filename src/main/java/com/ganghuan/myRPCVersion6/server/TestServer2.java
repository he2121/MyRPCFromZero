package com.ganghuan.myRPCVersion6.server;


import com.ganghuan.myRPCVersion6.service.BlogService;
import com.ganghuan.myRPCVersion6.service.BlogServiceImpl;
import com.ganghuan.myRPCVersion6.service.UserService;
import com.ganghuan.myRPCVersion6.service.UserServiceImpl;

public class TestServer2 {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 8900);
        // System.out.println("hahah");
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);
        RPCServer RPCServer = new NettyRPCServer(serviceProvider);

        RPCServer.start(8900);
    }
}
