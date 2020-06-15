/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.util.HashSet;
import java.util.LinkedList;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.ParamTypeBinaryDecoder;
import io.openliberty.wsoc.common.ParamTypeBinaryEncoder;
import io.openliberty.wsoc.common.ParamTypeTextDecoder;
import io.openliberty.wsoc.common.ParamTypeTextEncoder;

@ServerEndpoint(value = "/complexCoding", decoders = { ParamTypeTextDecoder.class, ParamTypeBinaryDecoder.class },
                encoders = { ParamTypeTextEncoder.class, ParamTypeBinaryEncoder.class })
public class ParamTypeCodingServerEP {

    @OnMessage
    public LinkedList<HashSet<String>> onMessage(LinkedList<HashSet<String>> decodedObject) {
        return decodedObject;
    }

    @OnMessage
    public HashSet<String> onMessage(HashSet<String> decodedObject) {
        return decodedObject;
    }

}
