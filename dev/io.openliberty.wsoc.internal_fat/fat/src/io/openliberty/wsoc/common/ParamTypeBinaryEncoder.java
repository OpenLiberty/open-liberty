/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ParamTypeBinaryEncoder implements Encoder.Binary<HashSet<String>> {

    public ParamTypeBinaryEncoder() {}

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public ByteBuffer encode(HashSet<String> arg0) throws EncodeException {
        ByteBuffer b = ByteBuffer.wrap(quickEncodeBinary(arg0).getBytes());
        return b;
    }

    public static String quickEncodeBinary(HashSet<String> data) {

        Iterator<String> i = data.iterator();
        return i.next();

    }

}
