/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
