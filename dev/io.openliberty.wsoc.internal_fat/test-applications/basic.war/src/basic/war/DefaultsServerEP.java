/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
