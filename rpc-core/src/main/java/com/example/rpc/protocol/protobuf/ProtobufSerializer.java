package com.example.rpc.protocol.protobuf;

import com.example.rpc.exception.RpcException;
import com.example.rpc.protocol.ISerializer;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.util.SchemaCache;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Protobuf序列化封装的辅助类
 */
public class ProtobufSerializer implements ISerializer {

    private static final ProtobufSerializer INSTANCE = new ProtobufSerializer();

    private ProtobufSerializer() {
    }

    public static ISerializer getInstance() {
        return INSTANCE;
    }

    private <T> int writeObject(LinkedBuffer buffer, T object, Schema<T> schema) {
        return ProtobufIOUtil.writeTo(buffer, object, schema);
    }

    protected <T> void parseObject(byte[] bytes, T template, Schema<T> schema) {
        ProtobufIOUtil.mergeFrom(bytes, template, schema);
    }

    private <T> void parseObject(InputStream in, T template, Schema<T> schema) {
        try {
            ProtobufIOUtil.mergeFrom(in, template, schema);
        } catch (IOException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public RpcRequest decodeRequest(InputStream inputStream) throws IOException {
        return decode(inputStream, new RpcRequest());
    }

    @Override
    public void encodeResponse(OutputStream outputStream, RpcResponse result) throws IOException {
        encode(outputStream, result);
    }

    @Override
    public RpcResponse decodeResponse(InputStream inputStream) throws IOException {
        return decode(inputStream, new RpcResponse());
    }

    @Override
    public void encodeRequest(OutputStream outputStream, RpcRequest request) throws IOException {
        encode(outputStream, request);
    }

    /**
     * 将对象序列化为二进制流
     *
     * @param out
     * @param object
     * @throws IOException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void encode(OutputStream out, T object) throws IOException {
        LinkedBuffer buffer = LinkedBuffer.allocate();
        Schema schema = null;
        if (null == object) {
            schema = SchemaCache.getSchema(Object.class);
        } else {
            schema = SchemaCache.getSchema(object.getClass());
        }
        int length = writeObject(buffer, object, schema);
        //IOUtils.writeInt(out, length);
        LinkedBuffer.writeTo(out, buffer);
    }

    /**
     * 将二进制流解析为对象
     *
     * @param in
     * @param template
     * @return
     * @throws IOException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T decode(InputStream in, T template) throws IOException {
        Schema schema = SchemaCache.getSchema(template.getClass());
        //int length = IOUtils.readInt(in);
        //byte[] bytes = new byte[length];
        //IOUtils.readFully(in, bytes, 0, length);
        parseObject(in, template, schema);
        return template;
    }
}
