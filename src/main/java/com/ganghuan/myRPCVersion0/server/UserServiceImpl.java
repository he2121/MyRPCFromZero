package com.ganghuan.myRPCVersion0.server;

import com.ganghuan.myRPCVersion0.common.User;
import com.ganghuan.myRPCVersion0.service.UserService;

import java.util.Random;

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
