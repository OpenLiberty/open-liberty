/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 * Binary Decoder
 * 
 * @author Rashmi Hunt
 */
public class BinaryDecoder implements Decoder.Binary<BinaryFormater> {

    public BinaryDecoder() {}

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public BinaryFormater decode(ByteBuffer byteBuffer) throws DecodeException {
        BinaryFormater formater = BinaryFormater.doDecoding(byteBuffer);
        return formater;

    }

    @Override
    public boolean willDecode(ByteBuffer arg0) {
        return true;
    }
}
