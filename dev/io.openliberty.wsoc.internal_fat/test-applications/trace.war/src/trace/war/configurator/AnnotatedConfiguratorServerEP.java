/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trace.war.configurator;

import java.util.ArrayList;
import java.util.Arrays;

import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

/**
 *
 */
public class AnnotatedConfiguratorServerEP {
    @ServerEndpoint(value = "/annotatedModifyHandshake", configurator = ServerConfigurator.class)
    public static class ConfiguratorTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {

        }

    }

    public static class ServerConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec,
                                    HandshakeRequest request,
                                    HandshakeResponse response) {

            // New header
            response.getHeaders().put("ConfiguratorHeader", new ArrayList<String>(Arrays.asList("SUCCESS")));

            // Existing header
            response.getHeaders().put("X-Powered-By", new ArrayList<String>(Arrays.asList("ONE", "TWO", "THREE")));

        }
    }

}
