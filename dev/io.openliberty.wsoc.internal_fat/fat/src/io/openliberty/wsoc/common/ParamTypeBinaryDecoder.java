/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.nio.ByteBuffer;
import java.util.HashSet;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class ParamTypeBinaryDecoder implements Decoder.Binary<HashSet<String>> {

    public ParamTypeBinaryDecoder() {}

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public HashSet<String> decode(ByteBuffer arg0) throws DecodeException {
        return quickDecodeBinary(new String(arg0.array()));
    }

    @Override
    public boolean willDecode(ByteBuffer arg0) {
        return true;
    }

    public static HashSet<String> quickDecodeBinary(String data) {
        HashSet<String> s = new HashSet<String>();
        s.add(data);

        return s;
    }
}
