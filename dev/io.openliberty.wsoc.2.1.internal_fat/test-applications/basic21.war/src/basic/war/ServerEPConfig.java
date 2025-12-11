/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package basic.war;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

/*
 * Echos messages sent to this endpoint while testing that the access modifier for 
 * ServerEndpointConfig.Configurator.getContainerDefaultConfigurator is now public
 * https://github.com/jakartaee/websocket/issues/207
 */
@ServerEndpoint(value = "/testEchoConfigurator")
public class ServerEPConfig {

    @OnOpen
    public void onOpen(final EndpointConfig epc) {
        ServerEndpointConfig cepc = (ServerEndpointConfig) epc;
        // Ensure that the getContainerDefaultConfigurator is accessible publicly with websocket 2.1
        cepc.getConfigurator().getContainerDefaultConfigurator();
    }

    @OnMessage
    public String echo(String input) {
        return input;
    }

}
