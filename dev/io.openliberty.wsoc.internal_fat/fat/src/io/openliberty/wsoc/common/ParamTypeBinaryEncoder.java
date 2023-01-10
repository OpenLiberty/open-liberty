/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
