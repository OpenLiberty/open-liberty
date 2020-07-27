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
