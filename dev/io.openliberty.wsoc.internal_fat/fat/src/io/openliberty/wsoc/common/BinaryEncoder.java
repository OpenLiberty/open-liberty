/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * Binary Encoder
 * 
 * @author Rashmi Hunt
 */
public class BinaryEncoder implements Encoder.Binary<BinaryFormater> {

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public java.nio.ByteBuffer encode(BinaryFormater arg0) throws EncodeException {
        return BinaryFormater.doEncoding(arg0);
    }

}
