/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.util.HashSet;
import java.util.LinkedList;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class ParamTypeTextDecoder implements Decoder.Text<LinkedList<HashSet<String>>> {
    public ParamTypeTextDecoder() {}

    @Override
    public void destroy() {}

    @Override
    public void init(EndpointConfig arg0) {}

    @Override
    public LinkedList<HashSet<String>> decode(String arg0) throws DecodeException {
        return quickDecode(arg0);
    }

    @Override
    public boolean willDecode(String arg0) {
        return true;
    }

    public static LinkedList<HashSet<String>> quickDecode(String data) {
        HashSet<String> s = new HashSet<String>();
        s.add(data);
        LinkedList<HashSet<String>> ll = new LinkedList<HashSet<String>>();
        ll.add(s);
        return ll;
    }

}
