/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.basic;

import java.util.Arrays;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

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
        System.out.println("DefaultsServerEP#getDecoders: " + returnText);
        return returnText;
    }
}
