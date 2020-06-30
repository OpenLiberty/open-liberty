/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import basic.war.coding.DecoderOne;
import basic.war.coding.DecoderTwo;
import basic.war.coding.FormatOne;

// Test case has 2 decoders which decodes of same data type (FormatOne). DecoderTwo returns false in it's willDecode() method, hence runtime should pick up the next decoder which is DecoderOne
// which has willDecode() method returning 'true'
@ServerEndpoint(value = "/willdecodetextendpoint", decoders = { DecoderTwo.class, DecoderOne.class },
                encoders = { basic.war.coding.EncoderOne.class })
public class WillDecodeTextServerEP {

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        //do nothing
    }

    @OnMessage
    public FormatOne decodeTextError(FormatOne decodedObject) {
        return decodedObject;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        // do nothing
    }

}
