package com.ganghuan.myRPCVersion3.client;

import com.ganghuan.myRPCVersion3.common.Blog;
import com.ganghuan.myRPCVersion3.common.RPCResponse;
import com.ganghuan.myRPCVersion3.common.User;
import com.ganghuan.myRPCVersion3.service.BlogService;
import com.ganghuan.myRPCVersion3.service.UserService;
import com.ganghuan.myRPCVersion3.service.UserServiceImpl;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;

public class TestClient {
    public static void main(String[] args) {
        // 构建一个使用java Socket/ netty/....传输的客户端
        RPCClient rpcClient = new NettyRPCClient("127.0.0.1", 8899);
        AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
        // 把这个客户端传入代理客户端
        RPCClientProxy rpcClientProxy = new RPCClientProxy(rpcClient);
        // 代理客户端根据不同的服务，获得一个代理类， 并且这个代理类的方法以或者增强（封装数据，发送请求）
        UserService userService = rpcClientProxy.getProxy(UserService.class);
        // 调用方法
        ChannelFuture future = userService.getUserByUserId(10);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("收到了");
                System.out.println(future.channel().attr(key).get().getData());
            }
        });
//        System.out.println("从服务端得到的user为：" + userByUserId);

//        User user = User.builder().userName("张三").id(100).sex(true).build();
//        Integer integer = userService.insertUserId(user);
//        System.out.println("向服务端插入数据："+integer);
//
//        BlogService blogService = rpcClientProxy.getProxy(BlogService.class);
//
//        Blog blogById = blogService.getBlogById(10000);
//        System.out.println("从服务端得到的blog为：" + blogById);

    }
}
