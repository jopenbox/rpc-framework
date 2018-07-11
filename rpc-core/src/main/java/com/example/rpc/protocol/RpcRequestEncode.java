package com.example.rpc.protocol;

import java.io.ByteArrayOutputStream;

import com.example.rpc.util.Constants;
import com.example.rpc.util.MySerializerFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * 请求编码
 */
public class RpcRequestEncode extends SimpleChannelHandler {

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        RpcRequest request = (RpcRequest) e.getMessage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        //先写入标示的魔数
        baos.write(Constants.MAGIC_BYTES);
        MySerializerFactory.getInstance(Constants.DEFAULT_RPC_CODE_MODE).encodeRequest(baos, request);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(baos.toByteArray());
        Channels.write(ctx, e.getFuture(), buffer);
    }
}
