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
package secure.war;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

//TODO - move this class into AnnotatedEndpoint once we get onMessage session argument passed in.
@ServerEndpoint(value = "/endpoints/annotatedSecureText", configurator = SecureTextServerEP.ServerConfigurator.class)
public class SecureTextServerEP {

    private HttpSession session = null;

    @OnMessage
    public void echoText(String val, Session sess) {
        session.invalidate();
    }

    @OnOpen
    public void onOpen(EndpointConfig epc) {
        ServerEndpointConfig cepc = (ServerEndpointConfig) epc;
        ServerConfigurator cc = (ServerConfigurator) cepc.getConfigurator();
        session = cc.session;
    }

    public static class ServerConfigurator extends ServerEndpointConfig.Configurator {

        public HttpSession session = null;

        @Override
        public void modifyHandshake(ServerEndpointConfig sec,
                                    HandshakeRequest request,
                                    HandshakeResponse response) {

            session = (HttpSession) request.getHttpSession();

            if (request.isUserInRole("AllAuthenticated")) {
                //MSN REMARK - when we figure this out - implement this.
            }
            else {

            }

        }
    }
}