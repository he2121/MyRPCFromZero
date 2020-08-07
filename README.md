# MyRPCFromZero

从零开始，手写一个RPC，跟随着这篇文档以及数个迭代版本的代码，由简陋到逐渐完备，让所有人都能看懂并且写出一个RPC框架。

本文档与代码都是本人第一次手写RPC的心路历程，会有理解的偏差与代码上的不完善，但更是由于这样，有着与新手对同样问题的疑惑，也许会使新手更容易理解这样做的缘故是啥。 

另外期待与你的**合作**：代码，帮助文档甚至rpc框架功能的完备

**学习建议**：

- 一定要实际上手敲代码
- 每一版本都有着对应独立的代码与文档，结合来看
- 每一版本前有一个背景知识，建议先掌握其相关概念再上手代码
- 每一个版本都有着要解决的问题与此版本的最大痛点，带着问题去写代码，并且与上个版本的代码进行比较差异



## RPC的概念

#### 背景知识

- RPC的基本概念，核心功能

![image-20200805001037799](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805124759206.png)

常见的RPC框架

#### Duboo基本功能

1. **远程通讯**
2. 基于接口方法的透明远程过程调用
3. 负载均衡
4. 服务注册中心

#### RPC过程

client 调用远程方法-> request序列化 -> 协议编码 -> 网络传输-> 服务端 -> 反序列化request -> 调用本地方法得到response -> 序列化 ->编码->…..



------

## 版本迭代过程

### 目录

从0开始的RPC的迭代过程：

- [version0版本](#0.一个最简单的RPC调用)：以不到百行的代码完成一个RPC例子
- [version1版本](#1.MyRPC版本1)：完善通用消息格式（request，response），客户端的动态代理完成对request消息格式的封装
- [version2版本](#2.MyRPC版本2)：支持服务端暴露多个服务接口， 服务端程序抽象化，规范化
- [version3版本](#3.MyRPC版本3)：使用高性能网络框架netty的实现，客户端代码的重构
- [version4版本](#4.MyRPC版本4)：自定义消息格式，支持多种序列化方式
- [version5版本](#):   服务器注册与发现的实现，zookeeper作为注册中心
- [version6版本](#):   自动注销与负载均衡的策略的实现
- [version7版本](#): ...



------



### 0.一个最简单的RPC调用

#### **背景知识**

- java基础
- java socket编程入门
- 项目使用maven搭建，暂时只引入了lombok包

#### 本节问题

- **什么是RPC，怎么完成一个RPC?**

一个RPC**最最最简单**的过程是客户端**调用**服务端的的一个方法, 服务端返回执行方法的返回值给客服端。接下来我会以一个从数据库里取数据的例子来进行一次模拟RPC过程的一个完整流程。

**假定**有以下这样一个服务：

服务端：

1. 有一个User表

 	2. UserServiceImpl 实现了UserService接口
 	3. UserService里暂时只有一个功能: getUserByUserId(Integer id)

客户端：

​	 传一个Id给服务端，服务端查询到User对象返回给客户端

#### 过程

1. 首先我们得有User对象，这是客户端与服务端都已知的，客户端需要得到这个pojo对象数据，服务端需要操作这个对象

```java
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    // 客户端和服务端共有的
    private Integer id;
    private String userName;
    private Boolean sex;
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
        Random random = new Random();
        User user = User.builder().userName(UUID.randomUUID().toString())
                .id(id)
                .sex(random.nextBoolean()).build();
        return user;
    }
}
```

4. 客户端建立Socket连接，传输Id给服务端，得到返回的User对象

```java
public class RPCClient {
    public static void main(String[] args) {
        try {
            // 建立Socket连接
            Socket socket = new Socket("127.0.0.1", 8899);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 传给服务器id
            objectOutputStream.writeInt(new Random().nextInt());
            objectOutputStream.flush();
            // 服务器查询数据，返回对应的对象
            User user  = (User) objectInputStream.readObject();
            System.out.println("服务端返回的User:"+user);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("客户端启动失败");
        }
    }
}
```

5. 服务端以BIO的方式监听Socket，如有数据，调用对应服务的实现类执行任务，将结果返回给客户端

```java
public class RPCServer {
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
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        // 读取客户端传过来的id
                        Integer id = ois.readInt();
                        User userByUserId = userService.getUserByUserId(id);
                        // 写入User对象给客户端
                        oos.writeObject(userByUserId);
                        oos.flush();
                    } catch (IOException e){
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

#### 结果：

![image-20200805001024797](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805001024797.png)

![image-20200805124759206](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805001037799.png)

#### 总结：

这个例子以不到百行的代码，实现了客户端与服务端的一个远程过程调用，非常适合上手，当然它是**及其不完善**的，甚至连消息格式都没有统一，我们将在接下来的版本更新中逐渐完善它。

#### 此RPC的最大痛点：

1. 只能调用服务端Service唯一确定的方法，如果有两个方法需要调用呢?（Reuest需要抽象）
2. 返回值只支持User对象，如果需要传一个字符串或者一个Dog，String对象呢（Response需要抽象）
3. 客户端不够通用，host，port， 与调用的方法都特定（需要抽象）



------



### 1.MyRPC版本1

#### 背景知识

- 反射
- 动态代理

#### 本节问题

- 如何使客户端请求远程方法支持多种?

- 如何使服务端返回值的类型多样?

#### 版本升级过程

**更新1**：定义了一个通用的Request的对象（消息格式）

```java
/**
 * 在上个例子中，我们的Request仅仅只发送了一个id参数过去，这显然是不合理的，
 * 因为服务端不会只有一个服务一个方法，因此只传递参数不会知道调用那个方法
 * 因此一个RPC请求中，client发送应该是需要调用的Service接口名，方法名，参数，参数类型
 * 这样服务端就能根据这些信息根据反射调用相应的方法
 * 还是使用java自带的序列化方式
 */
@Data
@Builder
public class RPCRequest implements Serializable {
    // 服务类名，客户端只知道接口名，在服务端中用接口名指向实现类
    private String interfaceName;
    // 方法名
    private String methodName;
    // 参数列表
    private Object[] params;
    // 参数类型
    private Class<?>[] paramsTypes;
}
```

**更新2：** 定义了一个通用的Response的对象（消息格式）

```java
/**
 * 上个例子中response传输的是User对象，显然在一个应用中我们不可能只传输一种类型的数据
 * 由此我们将传输对象抽象成为Object
 * Rpc需要经过网络传输，有可能失败，类似于http，引入状态码和状态信息表示服务调用成功还是失败
 */
@Data
@Builder
public class RPCResponse implements Serializable {
    // 状态信息
    private int code;
    private String message;
    // 具体数据
    private Object data;

    public static RPCResponse success(Object data) {
        return RPCResponse.builder().code(200).data(data).build();
    }
    public static RPCResponse fail() {
        return RPCResponse.builder().code(500).message("服务器发生错误").build();
    }
}
```

因此在网络传输过程都是request与response格式的数据了，客户端与服务器端就要负责封装与解析以上结构数据

**更新3：** 服务端接受request请求，并调用request中的对应的方法

```java
public interface UserService {
    // 客户端通过这个接口调用服务端的实现类
    User getUserByUserId(Integer id);
    // 给这个服务增加一个功能
    Integer insertUserId(User user);
}
```

服务端的实现类UserServiceImpl要实现增加的功能

```java
@Override
public Integer insertUserId(User user) {
    System.out.println("插入数据成功："+user);
    return 1;
}
```

服务端接受解析reuqest与封装发送response对象

```java
public class RPCServer {
    public static void main(String[] args) {

        UserServiceImpl userService = new UserServiceImpl();
        try {
            ServerSocket serverSocket = new ServerSocket(8899);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true){
                Socket socket = serverSocket.accept();
                // 开启一个线程去处理，这个类负责的功能太复杂，以后代码重构中，这部分功能要分离出来
                new Thread(()->{
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        // 读取客户端传过来的request
                        RPCRequest request = (RPCRequest) ois.readObject();
                        // 反射调用对应方法
                        Method method = userService.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
                        Object invoke = method.invoke(userService, request.getParams());
                        // 封装，写入response对象
                        oos.writeObject(RPCResponse.success(invoke));
                        oos.flush();
                    }catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
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

**更新4：** 客户端根据不同的Service进行动态代理：

代理对象增强的**公共行为**：把不同的Service方法**封装成统一的Request对象格式**，并且建立与Server的通信

1. 底层的通信

```java
public class IOClient {
    // 这里负责底层与服务端的通信，发送的Request，接受的是Response对象
    // 客户端发起一次请求调用，Socket建立连接，发起请求Request，得到响应Response
    // 这里的request是封装好的（上层进行封装），不同的service需要进行不同的封装， 客户端只知道Service接口，需要一层动态代理根据反射封装不同的Service
    public static RPCResponse sendRequest(String host, int port, RPCRequest request){
        try {
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            System.out.println(request);
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();

            RPCResponse response = (RPCResponse) objectInputStream.readObject();

            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println();
            return null;
        }
    }
}
```

2. 动态代理封装request对象

```java
@AllArgsConstructor
public class ClientProxy implements InvocationHandler {
    // 传入参数Service接口的class对象，反射封装成一个request
    private String host;
    private int port;


    // jdk 动态代理， 每一次代理对象调用方法，会经过此方法增强（反射获取request对象，socket发送至客户端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // request的构建，使用了lombok中的builder，代码简洁
        RPCRequest request = RPCRequest.builder().interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsTypes(method.getParameterTypes()).build();
        // 数据传输
        RPCResponse response = IOClient.sendRequest(host, port, request);
        //System.out.println(response);
        return response.getData();
    }
    <T>T getProxy(Class<T> clazz){
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
```

3. 客户端调用不同的方法

```java
public class RPCClient {
    public static void main(String[] args) {

        ClientProxy clientProxy = new ClientProxy("127.0.0.1", 8899);
        UserService proxy = clientProxy.getProxy(UserService.class);

        // 服务的方法1
        User userByUserId = proxy.getUserByUserId(10);
        System.out.println("从服务端得到的user为：" + userByUserId);
        // 服务的方法2
        User user = User.builder().userName("张三").id(100).sex(true).build();
        Integer integer = proxy.insertUserId(user);
        System.out.println("向服务端插入数据："+integer);
    }
}
```

#### 结果

![image-20200805163937195](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805163937195.png)

![image-20200805163959630](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805163959630.png)

#### 总结

1. 定义更加通用的消息格式：Request 与Response格式， 从此可能调用不同的方法，与返回各种类型的数据。
2. 使用了动态代理进行不同服务方法的Request的封装，
3. 客户端更加松耦合，不再与特定的Service，host，port绑定

#### 存在的痛点

1. 服务端我们直接绑定的是UserService服务，如果还有其它服务接口暴露呢?（多个服务的注册）
2. 服务端以BIO的方式性能是否太低，
3. 服务端功能太复杂：监听，处理。需要松耦合



------



### 2.MyRPC 版本2

#### 背景知识

- 代码解耦
- 线程池

#### 本节问题：

如果一个服务端要提供多个服务的接口， 例如新增一个BlogService，怎么处理?

```java
// 自然的想到用一个Map来保存，<interfaceName, xxxServiceImpl>
UserService userService = new UserServiceImpl();
BlogService blogService = new BlogServiceImpl();
Map<String, Object>.put("***.userService", userService);
Map<String, Object>.put("***.blogService", blogService);

// 此时来了一个request，我们就能从map中取出对应的服务
Object service = map.get(request.getInterfaceName())
```

#### 版本升级过程

**更新前的工作：** 更新一个新的服务接口样例和pojo类

```java
// 新的服务接口
public interface BlogService {
    Blog getBlogById(Integer id);
}
// 服务端新的服务接口实现类
public class BlogServiceImpl implements BlogService {
    @Override
    public Blog getBlogById(Integer id) {
        Blog blog = Blog.builder().id(id).title("我的博客").useId(22).build();
        System.out.println("客户端查询了"+id+"博客");
        return blog;
    }
}
// pojo类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Blog implements Serializable {
    private Integer id;
    private Integer useId;
    private String title;
}
```

**更新1：** HashMap<String, Object> 添加多个服务的实现类

```java
public class TestServer {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();
        Map<String, Object> serviceProvide = new HashMap<>();
        // 暴露两个服务接口， 即在RPCServer中加一个HashMap
        serviceProvide.put("com.ganghuan.myRPCVersion2.service.UserService",userService);
        serviceProvide.put("com.ganghuan.myRPCVersion2.service.BlogService",blogService);

        RPCServer RPCServer = new SimpleRPCRPCServer(serviceProvide);
        RPCServer.start(8899);
    }
}
// 这里先不去讨论实现其中的细节，因为这里还应该进行优化，我们先去把服务端代码松耦合，再回过来讨论
```

**更新2：** 服务端代码重构

1. 抽象RPCServer，开放封闭原则

```java
// 把RPCServer抽象成接口，以后的服务端实现这个接口即可
public interface RPCServer {
    void start(int port);
    void stop();
}
```

2. RPCService简单版本的实现

```java
/**
 * 这个实现类代表着java原始的BIO监听模式，来一个任务，就new一个线程去处理
 * 处理任务的工作见WorkThread中
 */
public class SimpleRPCRPCServer implements RPCServer {
    // 存着服务接口名-> service对象的map
    private Map<String, Object> serviceProvide;

    public SimpleRPCRPCServer(Map<String,Object> serviceProvide){
        this.serviceProvide = serviceProvide;
    }

    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true){
                Socket socket = serverSocket.accept();
                // 开启一个新线程去处理
                new Thread(new WorkThread(socket,serviceProvide)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("服务器启动失败");
        }
    }

    public void stop(){
    }
}
```

3. 为了加强性能，我们还提供了**线程池版**的实现

```java
public class ThreadPoolRPCRPCServer implements RPCServer {
    private final ThreadPoolExecutor threadPool;
    private Map<String, Object> serviceProvide;

    public ThreadPoolRPCRPCServer(Map<String, Object> serviceProvide){
        threadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                1000, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        this.serviceProvide = serviceProvide;
    }
    public ThreadPoolRPCRPCServer(Map<String, Object> serviceProvide, int corePoolSize,
                                  int maximumPoolSize,
                                  long keepAliveTime,
                                  TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue){
        
        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.serviceProvide = serviceProvide;
    }
    @Override
    public void start(int port) {
        System.out.println("服务端启动了");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while(true){
                Socket socket = serverSocket.accept();
                threadPool.execute(new WorkThread(socket,serviceProvide));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
    }
}
```

4. 工作任务类，从服务端代码分离出来，简化服务端代码，单一职责原则

```java
/**
 * 这里负责解析得到的request请求，执行服务方法，返回给客户端
 * 1. 从request得到interfaceName 2. 根据interfaceName在serviceProvide Map中获取服务端的实现类
 * 3. 从request中得到方法名，参数， 利用反射执行服务中的方法 4. 得到结果，封装成response，写入socket
 */
@AllArgsConstructor
public class WorkThread implements Runnable{
    private Socket socket;
    private Map<String, Object> serviceProvide;
    @Override
    public void run() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            // 读取客户端传过来的request
            RPCRequest request = (RPCRequest) ois.readObject();
            // 反射调用服务方法获得返回值
            RPCResponse response = getResponse(request);
            //写入到客户端
            oos.writeObject(response);
            oos.flush();
        }catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("从IO中读取数据错误");
        }
    }

    private RPCResponse getResponse(RPCRequest request){
        // 得到服务名
        String interfaceName = request.getInterfaceName();
        // 得到服务端相应服务实现类
        Object service = serviceProvide.get(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            return RPCResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RPCResponse.fail();
        }
    }
}
```

服务端代码第一次重构完毕。 

**更新3：** 服务暴露类，这里回到了更新1**，我们发现服务接口名是我们**直接手写的，这里其实可以利用反射自动得到

```java
/**
 * 之前这里使用Map简单实现的
 * 存放服务接口名与服务端对应的实现类
 * 服务启动时要暴露其相关的实现类0
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
```

前面服务端的代码中有Sevicerprovide 这个HashMap的地方需要改成ServiProvider，比如

```java
public class TestServer {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

//        Map<String, Object> serviceProvide = new HashMap<>();
//        serviceProvide.put("com.ganghuan.myRPCVersion2.service.UserService",userService);
//        serviceProvide.put("com.ganghuan.myRPCVersion2.service.BlogService",blogService);
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);
        
        RPCServer RPCServer = new SimpleRPCRPCServer(serviceProvider);
        RPCServer.start(8899);
    }
}
```

#### 结果

```java
// 客户中添加新的测试用例        
BlogService blogService = rpcClientProxy.getProxy(BlogService.class);
Blog blogById = blogService.getBlogById(10000);
System.out.println("从服务端得到的blog为：" + blogById);
```



![image-20200805221430990](http://ganghuan.oss-cn-shenzhen.aliyuncs.com/img/image-20200805221430990.png)

#### 总结：

在一版本中，我们重构了服务端的代码，代码更加简洁，

 添加线程池版的服务端的实现，性能应该会有所提升（未测）

并且服务端终于能够提供不同服务了， 功能更加完善，不再鸡肋

#### 此RPC最大的痛点

1. 传统的BIO与线程池网络传输性能低



------



### 3.MyRPC版本3

#### 背景知识

- netty高性能网络框架的使用

#### 本节问题

如何提升这个rpc的性能? 可以从两个方面入手，网络传输从BIO到NIO，序列化要减少字节流长度，提高序列化反序列化效率

知名的rpc框架：dubbo， grpc都是使用netty底层进行通信的

#### 升级过程：

**前提：** maven pom.xml文件引入netty

```java
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.51.Final</version>
</dependency>
```

**升级1：** 重构客户端代码

客户端的代码太乱了， 我们先进行代码重构，才有利于后面使用netty的方式实现客户端，使之不同方式网络连接的客户端有着同样的结构，同样的api

假如我们现在已经有了两个客户端：SimpleRPCClient(使用java BIO的方式)， NettyRPCClient（使用netty进行网络传输），那么它们两者的共性是啥?**发送请求与得到response**是共性， 而建立连接与发送请求的方式是不同点。

```java
// 共性抽取出来
public interface RPCClient {
    RPCResponse sendRequest(RPCRequest response);
}
// SimpleRPCClient实现这个接口，不同的网络方式有着不同的实现
@AllArgsConstructor
public class SimpleRPCClient implements RPCClient{
    private String host;
    private int port;

    // 客户端发起一次请求调用，Socket建立连接，发起请求Request，得到响应Response
    // 这里的request是封装好的，不同的service需要进行不同的封装， 客户端只知道Service接口，需要一层动态代理根据反射封装不同的Service
    public RPCResponse sendRequest(RPCRequest request) {
        try {
            // 发起一次Socket连接请求
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
```

而**RPCClientProxy**类中需要加入一个RPCClient类变量即可， 传入不同的client(simple,netty), 即可调用公共的接口sendRequest发送请求，所以客户端代码结构很清晰了:

- **RPCClient**: 不同的网络连接，网络传输方式的客户端分别实现这个接口

- **XXXRPCClient:** 具体实现类

- **RPCClientProxy：** 动态代理Service类，封装不同的Service请求为Request对象，并且持有一个RPCClient对象，负责与服务端的通信， 

由此，客户端代码重构完毕，结构更为清晰，一个使用用例为：

```java
// 构建一个使用java Socket传输的客户端
SimpleRPCClient simpleRPCClient = new SimpleRPCClient("127.0.0.1", 8899);
// 把这个客户端传入代理客户端
RPCClientProxy rpcClientProxy = new RPCClientProxy(simpleRPCClient);
// 代理客户端根据不同的服务，获得一个代理类， 并且这个代理类的方法以或者增强（封装数据，发送请求）
UserService userService = rpcClientProxy.getProxy(UserService.class);
// 调用方法
User userByUserId = userService.getUserByUserId(10);
```

**升级2：** 使用netty方式传输数据：实现NettyRPCServer， NettyRPCCLient，这里建议先学习下netty的启动代码

**netty 服务端的实现**

```java
/**
 * 实现RPCServer接口，负责监听与发送数据
 */
@AllArgsConstructor
public class NettyRPCServer implements RPCServer {
    private ServiceProvider serviceProvider;
    @Override
    public void start(int port) {
        // netty 服务线程组boss负责建立连接， work负责具体的请求
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        System.out.printf("Netty服务端启动了...");
        try {
            // 启动netty服务器
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 初始化
            serverBootstrap.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer(serviceProvider));
            // 同步阻塞
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            // 死循环监听
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
    }
}
```

netty server初始化类

```java
/**
 * 初始化，主要负责序列化的编码解码， 需要解决netty的粘包问题
 */
@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 消息格式 [长度][消息体], 解决粘包问题
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
        // 计算当前待发送消息的长度，写入到前4个字节中
        pipeline.addLast(new LengthFieldPrepender(4));

        // 这里使用的还是java 序列化方式， netty的自带的解码编码支持传输这种结构
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));

        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }
}
```

netty server具体的handler

```java
/**
 * 因为是服务器端，我们知道接受到请求格式是RPCRequest
 * Object类型也行，强制转型就行
 */
@AllArgsConstructor
public class NettyRPCServerHandler extends SimpleChannelInboundHandler<RPCRequest> {
    private ServiceProvider serviceProvider;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCRequest msg) throws Exception {
        //System.out.println(msg);
        RPCResponse response = getResponse(msg);
        ctx.writeAndFlush(response);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    RPCResponse getResponse(RPCRequest request) {
        // 得到服务名
        String interfaceName = request.getInterfaceName();
        // 得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            return RPCResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RPCResponse.fail();
        }
    }
}
```

客户端netty的实现

```java
/**
 * 实现RPCClient接口
 */
public class NettyRPCClient implements RPCClient {
    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;
    private String host;
    private int port;
    public NettyRPCClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    // netty客户端初始化，重复使用
    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }

    /**
     * 这里需要操作一下，因为netty的传输都是异步的，你发送request，会立刻返回， 而不是想要的相应的response
     */
    @Override
    public RPCResponse sendRequest(RPCRequest request) {
        try {
            ChannelFuture channelFuture  = bootstrap.connect(host, port).sync();
            Channel channel = channelFuture.channel();
            // 发送数据
            channel.writeAndFlush(request);
            channel.closeFuture().sync();
            // 阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
            // AttributeKey是，线程隔离的，不会由线程安全问题。
            // 实际上不应通过阻塞，可通过回调函数
            AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
            RPCResponse response = channel.attr(key).get();

            System.out.println(response);
            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

初始化： 与服务端一致，就不贴代码了

ClientHandler设计：

```java
public class NettyClientHandler extends SimpleChannelInboundHandler<RPCResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCResponse msg) throws Exception {
        // 接收到response, 给channel设计别名，让sendRequest里读取response
        AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
        ctx.channel().attr(key).set(msg);
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

#### 结果：

![image-20200807172435854](D:\笔记\netty\img\image-20200807172435854.png)

![image-20200807172418651](D:\笔记\netty\img\image-20200807172418651.png)

#### 总结

此版本我们完成了客户端的重构，使之能够支持多种版本客户端的扩展

使用netty实现了客户端与服务端的通信

#### 此RPC最大痛点

1. java自带序列化方式（Java序列化写入不仅是完整的类名，也包含整个类的定义，包含所有被引用的类），不够通用，不够高效