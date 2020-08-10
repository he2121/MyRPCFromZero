package com.ganghuan.myRPCVersion5.client;


import com.ganghuan.myRPCVersion5.common.RPCRequest;
import com.ganghuan.myRPCVersion5.common.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
