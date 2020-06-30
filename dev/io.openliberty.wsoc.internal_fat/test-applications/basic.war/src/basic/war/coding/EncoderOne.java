/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war.coding;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class EncoderOne implements Encoder.Text<FormatOne> {

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public String encode(FormatOne arg0) throws EncodeException {
        String encodedText = FormatOne.doEncoding(arg0);
        if (encodedText.equals("Error encoder case")) {
            throw new EncodeException(encodedText, "can not encode");
        }
        return encodedText;
    }
}
