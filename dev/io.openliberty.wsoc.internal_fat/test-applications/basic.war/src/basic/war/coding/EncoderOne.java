/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
