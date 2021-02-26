/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
