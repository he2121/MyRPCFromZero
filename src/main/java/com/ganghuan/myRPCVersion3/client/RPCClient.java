package com.ganghuan.myRPCVersion3.client;


import com.ganghuan.myRPCVersion3.common.RPCRequest;
import io.netty.channel.ChannelFuture;

public interface RPCClient {
    ChannelFuture sendRequest(RPCRequest request);
}
