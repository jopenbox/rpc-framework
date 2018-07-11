package com.example.rpc.protocol.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.example.rpc.protocol.ISerializer;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * Hessian序列化封装的辅助类
 */
public class HessianSerializer implements ISerializer {

    private static final HessianSerializer INSTANCE = new HessianSerializer();

    private HessianSerializer() {
    }

    public static ISerializer getInstance() {
        return INSTANCE;
    }

    @Override
    public RpcRequest decodeRequest(InputStream inputStream) throws IOException {
        Hessian2Input in = new Hessian2Input(inputStream);
        return (RpcRequest) in.readObject();
    }

    @Override
    public void encodeResponse(OutputStream outputStream, RpcResponse result) throws IOException {
        Hessian2Output out = new Hessian2Output(outputStream);
        out.writeObject(result);
        out.flush();
    }

    @Override
    public RpcResponse decodeResponse(InputStream inputStream) throws IOException {
        Hessian2Input in = new Hessian2Input(inputStream);
        return (RpcResponse) in.readObject();
    }

    @Override
    public void encodeRequest(OutputStream outputStream, RpcRequest request) throws IOException {
        Hessian2Output out = new Hessian2Output(outputStream);
        out.writeObject(request);
        out.flush();
    }
}
