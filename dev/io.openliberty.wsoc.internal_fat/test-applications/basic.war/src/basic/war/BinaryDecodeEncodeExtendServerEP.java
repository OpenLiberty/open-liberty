/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
