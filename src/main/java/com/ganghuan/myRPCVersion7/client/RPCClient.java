package com.ganghuan.myRPCVersion7.client;


import com.ganghuan.myRPCVersion7.common.RPCRequest;
import com.ganghuan.myRPCVersion7.common.RPCResponse;

public interface RPCClient {
    RPCResponse sendRequest(RPCRequest request);
    void createWatch(String path);

}
