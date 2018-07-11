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
 * 响应编码
 */
public class RpcResponseEncode extends SimpleChannelHandler {

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        RpcResponse response = (RpcResponse) e.getMessage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);
        // 先写入标示的魔数
        baos.write(Constants.MAGIC_BYTES);
        MySerializerFactory.getInstance(Constants.DEFAULT_RPC_CODE_MODE).encodeResponse(baos, response);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(baos.toByteArray());
        Channels.write(ctx, e.getFuture(), buffer);
    }
}
