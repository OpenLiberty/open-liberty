/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.basic;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import javax.websocket.server.ServerEndpoint;

import javax.websocket.server.PathParam;

@ServerEndpoint(value = "/annotatedByteArray/{boolean-var}")
public class ByteArrayTest {

    @OnOpen
    public void onOpen(final Session session) {

    }

    //test which shows boolean pair, session, @PathParam and actual message. Parameters can be in any order
    @OnMessage
    public byte[] echoText(boolean last, Session session, @PathParam("boolean-var") boolean booleanVar, byte[] data) { //session, msg and last can be at different param index
        if (session != null && last && booleanVar) {
            System.out.println("ByteArrayTest#echoText: " + data);
            return data;
        } else {
            return null;
        }
    }

}
