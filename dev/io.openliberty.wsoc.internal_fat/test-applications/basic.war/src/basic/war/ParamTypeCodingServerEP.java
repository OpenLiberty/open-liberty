/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
