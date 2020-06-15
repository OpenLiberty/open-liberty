/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war.coding;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class DecoderOne implements Decoder.Text<FormatOne> {

    public DecoderOne() {}

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public FormatOne decode(String arg0) throws DecodeException {
        if (arg0.equals("Error decoder case")) { //to test decoder error path
            throw new DecodeException(arg0, "can not decode");
        }
        FormatOne fo = FormatOne.doDecoding(arg0);
        return fo;

    }

    @Override
    public boolean willDecode(String arg0) {
        return true;
    }

}
