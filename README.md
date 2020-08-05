# MyRPCFromZero
从零开始，手写一个RPC，任何人都能看懂

## RPC

![image-20200805001037799](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805124759206.png)

### Duboo基本功能

1. **远程通讯**
2. 基于接口方法的透明远程过程调用
3. 负载均衡
4. 服务注册中心

### RPC过程

client 调用远程方法-> request序列化 -> 协议编码 -> 网络传输-> 服务端 -> 反序列化request -> 调用本地方法得到response -> 序列化 ->编码->…..

## 目录

从0开始的RPC的迭代过程：

[version0版本](#0 一个最简单的RPC调用)

[version1版本](#)

[version2版本](#)

[version3版本](#)

## 0 一个最简单的RPC调用

一个RPC**最最最简单**的过程是客户端**调用**服务端的的一个方法, 服务端返回执行方法的返回值给客服端。接下来我会以一个从数据库里取数据的例子来进行一次模拟RPC过程的一个完整流程。



假定有以下这样一个服务：

服务端：

1. 有一个User表

 	2. UserServiceImpl 实现了UserService接口
 	3. UserService里暂时只有一个功能: getUserByUserId(Integer id)

客户端：

	1. 传一个Id给服务端，服务端查询到User对象返回给客户端

### 过程

1. 首先我们得有User对象，这是客户端与服务端都已知的，客户端需要得到这个pojo对象数据，服务端需要操作这个对象

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    // 客户端和服务端共有的，模拟RPC中需要返回的message
    private Integer id;
    private String userName;
    private Boolean sex对象
}
```

2. 定义客户端需要调用，服务端需要提供的服务接口

```java
public interface UserService {
    // 客户端通过这个接口调用服务端的实现类
    User getUserByUserId(Integer id);
}
```

3. 服务端需要实现Service接口的功能

```java
public class UserServiceImpl implements UserService {
    @Override
    public User getUserByUserId(Integer id) {
        System.out.println("客户端查询了"+id+"的用户");
        // 模拟从数据库中取用户的行为
        User user = new User();
        user.setId(id);
        user.setUserName("he2121");
        // 增加一点随机性
        boolean sex = new Random().nextBoolean();
        user.setSex(sex);
        return user;
    }
}
```

4. 客户端建立Socket连接，传输Id给服务端，得到返回的User对象

```java
public class MyRPCClient {
    public static void main(String[] args) {
        try {
            // 建立Socket连接，这里的Socket的host与port每个客户端都可能有差异，需要抽象出来
            Socket socket = new Socket("127.0.0.1", 8899);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 传给服务器id
            objectOutputStream.writeInt(new Random().nextInt());
            objectOutputStream.flush();
            // 服务器查询数据，返回对应的对象
            Object o = objectInputStream.readObject();
            User user = (User) o;
            System.out.println(user);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("客户端启动失败");
        }
    }
}

```

5. 服务端以BIO的方式监听Socket，如有数据，调用对应服务的实现类执行任务，将结果返回给客户端

```java
public class MyRPCServer {
    public static void main(String[] args) {
        UserServiceImpl userService = new UserServiceImpl();
        try {
            ServerSocket serverSocket = new ServerSocket(8899);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true){
                Socket socket = serverSocket.accept();
                // 开启一个线程去处理
                new Thread(()->{
                    try {
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                        // 读取客户端传过来的id
                        Integer id = objectInputStream.readInt();
                        // 这个服务在这里是确定的，但很明显，一个server支持多种服务中的多种方法，需要抽象
                        User userByUserId = userService.getUserByUserId(id);
                        // 写入User对象给客户端
                        objectOutputStream.writeObject(userByUserId);
                        objectOutputStream.flush();
                    }catch (IOException e){
                        e.printStackTrace();
                        System.out.println("从IO中读取数据错误");
                    }
                }).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("服务器启动失败");
        }
    }
}

```

### 结果：

![image-20200805001024797](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805001024797.png)

![image-20200805124759206](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805001037799.png)





### 此RPC的最大痛点：

1. 只能调用服务端Service唯一确定的方法，如果有两个方法需要调用呢?（Reuest需要抽象）
2. 返回值只支持User对象，如果需要传一个字符串或者一个Dog对象呢（Response需要抽象）
3. 服务端使用NIO的方式监听Socket（效率低）



