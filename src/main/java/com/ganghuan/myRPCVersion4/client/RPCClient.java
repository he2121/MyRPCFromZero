package com.ganghuan.myRPCVersion4.client;


import com.ganghuan.myRPCVersion4.common.RPCRequest;
import com.ganghuan.myRPCVersion4.common.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
