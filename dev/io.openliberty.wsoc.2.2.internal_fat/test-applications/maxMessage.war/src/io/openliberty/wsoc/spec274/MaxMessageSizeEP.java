/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.spec274;

import jakarta.websocket.OnMessage;

import jakarta.websocket.server.ServerEndpoint;

/*
 * Echos messages sent to this endpoint.
 * Used for testing MaxMessageTest
 */
@ServerEndpoint(value = "/testMessageSize")
public class MaxMessageSizeEP {

    // Should throw a DeploymentException for maxMessageSize being larger than Integer.MAX_VALUE
    @OnMessage(maxMessageSize = 2147483648L)
    public void onMsg(String msg) {}

}
