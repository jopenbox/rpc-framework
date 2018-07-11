package com.example.rpc.util;

import com.example.rpc.protocol.hessian.HessianSerializer;
import com.example.rpc.protocol.ISerializer;
import com.example.rpc.protocol.protobuf.ProtobufSerializer;

public class MySerializerFactory {

    // protobuf协议
    private static final String PROTOBUF ="protobuf";
    // hessian协议
    private static final String HESSIAN ="hessian";

    @SuppressWarnings("all")
    public final static ISerializer getInstance(String mode) {
        if (PROTOBUF.equals(mode)) {
            return ProtobufSerializer.getInstance();
        } else if (HESSIAN.equals(mode)) {
            return HessianSerializer.getInstance();
        } else {
            return null;
        }
    }
}
