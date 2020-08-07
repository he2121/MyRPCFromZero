package com.ganghuan.myRPCVersion3.client;


import com.ganghuan.myRPCVersion3.common.Blog;
import com.ganghuan.myRPCVersion3.common.RPCRequest;
import com.ganghuan.myRPCVersion3.common.RPCResponse;
import com.ganghuan.myRPCVersion3.common.User;
import com.ganghuan.myRPCVersion3.service.BlogService;
import com.ganghuan.myRPCVersion3.service.UserService;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@AllArgsConstructor
public class SimpleRPCClient implements RPCClient{
    private String host;
    private int port;

    // 客户端发起一次请求调用，Socket建立连接，发起请求Request，得到响应Response
    // 这里的request是封装好的，不同的service需要进行不同的封装， 客户端只知道Service接口，需要一层动态代理根据反射封装不同的Service
    public RPCResponse sendRequest(RPCRequest request) {
        try {
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            System.out.println(request);
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();

            RPCResponse response = (RPCResponse) objectInputStream.readObject();

            //System.out.println(response.getData());
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println();
            return null;
        }
    }
}
