package com.ganghuan.myRPCVersion6.loadbalance;

import java.util.List;

/**
 * 给服务器地址列表，根据不同的负载均衡策略选择一个
 */
public interface LoadBalance {
    String balance(List<String> addressList);
}