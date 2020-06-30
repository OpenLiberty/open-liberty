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
package basic.war.coding;

import java.io.IOException;
import java.io.Writer;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import io.openliberty.wsoc.common.Constants;

/**
 *
 */
public class EncoderTextStreamGeneric<T> implements Encoder.TextStream<T> {

    @Override
    public void encode(T object, Writer writer) throws EncodeException {
        try {
            writer.write(Constants.ENCODER_GENERIC_SUCCESS);
            writer.close();
        } catch (IOException e) {
            System.out.println("Caught exception in EncoderTextStream.encode: " + e.toString());
        }
    }

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

}
