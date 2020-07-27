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

import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.BinaryDecoderOneExtend;
import io.openliberty.wsoc.common.BinaryDecoderTwoExtend;
import io.openliberty.wsoc.common.BinaryEncoderExtend;
import io.openliberty.wsoc.common.BinaryFormater;

// This tests application defined binary encoder and multiple binary decoder inheritance
@ServerEndpoint(value = "/BinaryDecodeEncodeExtend", decoders = { BinaryDecoderOneExtend.class, BinaryDecoderTwoExtend.class },
                encoders = { BinaryEncoderExtend.class
                })
public class BinaryDecodeEncodeExtendServerEP {

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {}

    //Input:  Payload from the client is ByteBuffer. Websocket implementation converts ByteBuffer to BinaryFormater based on decoder, BinaryDecoder
    //Output: Output result of this method is BinaryFormater.  Websocket implementation converts BinaryFormater to ByteBuffer (payload) based on the encoder, BinaryEncoder
    @OnMessage
    public BinaryFormater decodeTextSendBackEncodedText(BinaryFormater decodedObject) {
        String returnText = null;
        if (decodedObject != null) {
            returnText = "Result is " + decodedObject.getData();
        } else {
            returnText = "error";
        }
        // ByteBuffer byteBuffer = ByteBuffer.wrap(returnText.getBytes());
        BinaryFormater returnFormater = new BinaryFormater(returnText);
        return returnFormater;
    }
}
