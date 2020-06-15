/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.util.Arrays;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.BinaryStreamDecoder;

/**
 *
 */
@ServerEndpoint(value = "/defaults", decoders = { BinaryStreamDecoder.class })
public class DefaultsServerEP {
    @OnMessage
    public String getDefaults(String input) {
        if (input.equals("decoders")) {
            return getDecoders();
        }
        return input;
    }

    private String getDecoders() {
        ServerEndpoint serverEP = (ServerEndpoint) getClass().getAnnotations()[0];
        String returnText = Arrays.asList(serverEP.decoders()).toString();
        return returnText;
    }
}
