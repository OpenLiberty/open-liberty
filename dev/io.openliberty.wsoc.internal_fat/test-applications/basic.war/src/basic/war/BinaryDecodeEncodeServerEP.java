/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.DecodeException;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import basic.war.coding.DecoderOne;
import basic.war.coding.EncoderOne;
import basic.war.coding.FormatOne;

import io.openliberty.wsoc.common.BinaryDecoder;
import io.openliberty.wsoc.common.BinaryEncoder;
import io.openliberty.wsoc.common.BinaryFormater;

// This tests application defined binary encoder and binary decoder
@ServerEndpoint(value = "/BinaryDecodeEncode", decoders = { BinaryDecoder.class, DecoderOne.class },
                encoders = { BinaryEncoder.class,
                            EncoderOne.class })
public class BinaryDecodeEncodeServerEP {

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

    //Case 1) See DecodeOne class. onMessage will not get invoked.  DecodeOne.decode() throws exception prior to invoking onMessage which
    //gets passed onto to belowonError(..) method
    //case 2) See EncodeOne class. this onMessage will be invoked.  EncodeOne.encode()  throws EncodeException which gets passed onto to below 
    //onError(..) method
    @OnMessage
    public FormatOne decodeTextError(FormatOne decodedObject) {
        return decodedObject;
    }

    // Using the OnClose annotation will cause this method to be called when the WebSocket Session is being closed.
    @OnClose
    public void onClose(Session session, CloseReason reason) {

        try {
            java.lang.Thread.sleep(250);
        } catch (Exception e) {
        }

    }

    // Using the OnError annotation will cause this method to be called when the WebSocket Session has an error to report. For the Alpha version
    // of the WebSocket implementation on Liberty, this will not be called on error conditions.
    @OnError
    public void onError(final Session session, Throwable t) {
        if (session != null && t != null) {
            try {
                if (t instanceof DecodeException) {
                    if (((DecodeException) t).getText().equals("Error decoder case")) {
                        session.getBasicRemote().sendText("successfull");
                    }
                } else if (t instanceof EncodeException) {
                    String errorData = (String) ((EncodeException) t).getObject();
                    if (errorData.equals("Error encoder case")) {
                        session.getBasicRemote().sendText("successfull");
                    }
                } else
                    session.getBasicRemote().sendText("unsuccessfull");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
