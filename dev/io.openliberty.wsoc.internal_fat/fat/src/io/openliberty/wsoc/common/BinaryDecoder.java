/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
