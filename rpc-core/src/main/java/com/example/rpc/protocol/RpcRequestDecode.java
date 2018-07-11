package com.example.rpc.protocol;

import com.example.rpc.exception.RpcException;
import com.example.rpc.util.Constants;
import com.example.rpc.util.MySerializerFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * 请求解码
 */
public class RpcRequestDecode extends FrameDecoder {

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 2) {
            return null;
        }
        byte byte1 = buffer.readByte();
        byte byte2 = buffer.readByte();
        if (byte1 != Constants.MAGIC_HIGH || byte2 != Constants.MAGIC_LOW) {
            throw new RpcException("RpcRequestDecode: magic number not right");
        }
        ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
        return MySerializerFactory.getInstance(Constants.DEFAULT_RPC_CODE_MODE).decodeRequest(in);
    }
}
