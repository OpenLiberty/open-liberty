/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

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
    public void init(EndpointConfig arg0) {}

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
        // TODO Auto-generated method stub

    }

}
