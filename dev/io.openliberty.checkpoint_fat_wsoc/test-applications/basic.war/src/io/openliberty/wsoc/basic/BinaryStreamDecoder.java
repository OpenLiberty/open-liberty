/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.basic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.websocket.DecodeException;
import javax.websocket.Decoder.BinaryStream;
import javax.websocket.EndpointConfig;

/**
 *
 */
public class BinaryStreamDecoder implements BinaryStream<CustomString> {
    @Override
    public void init(EndpointConfig arg0) {
    }

    @Override
    public CustomString decode(InputStream instream) throws DecodeException {
        BufferedReader buffReader = new BufferedReader(new InputStreamReader(instream));
        String line = null;
        try {
            line = buffReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CustomString customString = new CustomString();
        customString.set(line);
        return customString;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.websocket.Decoder#destroy()
     */
    @Override
    public void destroy() {
        // Do nothing.
    }

}
