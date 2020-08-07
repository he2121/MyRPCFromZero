package com.ganghuan.myRPCVersion3.client;


import com.ganghuan.myRPCVersion3.common.RPCRequest;
import com.ganghuan.myRPCVersion3.common.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
}
