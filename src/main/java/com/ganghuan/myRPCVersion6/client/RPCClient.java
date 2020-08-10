package com.ganghuan.myRPCVersion6.client;


import com.ganghuan.myRPCVersion6.common.RPCRequest;
import com.ganghuan.myRPCVersion6.common.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
