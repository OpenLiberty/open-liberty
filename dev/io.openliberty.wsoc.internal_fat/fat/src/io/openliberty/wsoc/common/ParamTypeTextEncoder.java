/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class ParamTypeTextEncoder implements Encoder.Text<LinkedList<HashSet<String>>> {

    public ParamTypeTextEncoder() {}

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public String encode(LinkedList<HashSet<String>> arg0) throws EncodeException {
        return quickEncode(arg0);
    }

    public static String quickEncode(LinkedList<HashSet<String>> data) {
        HashSet<String> hs = data.get(0);
        Iterator<String> i = hs.iterator();
        return i.next();

    }

}
