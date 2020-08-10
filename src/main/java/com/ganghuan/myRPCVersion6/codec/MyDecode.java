package com.ganghuan.myRPCVersion6.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 按照自定义的消息格式解码数据
 */
@AllArgsConstructor
public class MyDecode extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 1. 读取消息类型
        short messageType = in.readShort();
        // 现在还只支持request与response请求
        if(messageType != MessageType.REQUEST.getCode() &&
                messageType != MessageType.RESPONSE.getCode()){
            System.out.println("暂不支持此种数据");
            return;
        }
        // 2. 读取序列化的类型
        short serializerType = in.readShort();
        // 根据类型得到相应的序列化器
        Serializer serializer = Serializer.getSerializerByCode(serializerType);
        if(serializer == null)throw new RuntimeException("不存在对应的序列化器");
        // 3. 读取数据序列化后的字节长度
        int length = in.readInt();
        // 4. 读取序列化数组
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        // 用对应的序列化器解码字节数组
        Object deserialize = serializer.deserialize(bytes, messageType);
        out.add(deserialize);
    }
}