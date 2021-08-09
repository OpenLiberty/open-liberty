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
package io.openliberty.wsoc.endpoints.client.trace;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public abstract class ProgrammaticConfiguratorClientEP extends Endpoint implements TestHelper {

    public static class ConfiguratorTest extends ProgrammaticConfiguratorClientEP {

        @Override
        public void onOpen(Session session, EndpointConfig epc) {

            ClientEndpointConfig cepc = (ClientEndpointConfig) epc;
            ClientConfigurator cc = (ClientConfigurator) cepc.getConfigurator();
            if (cc.success) {
                _wtr.overwriteSingleMessage("SUCCESS");
            } else {
                _wtr.overwriteSingleMessage("Failure, configurator did not modify request.");
            }
            _wtr.terminateClient();

        }

    }

    public WsocTestContext _wtr = null;

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

    public static class ClientConfigurator extends ClientEndpointConfig.Configurator {
        public boolean success = false;

        @Override
        public void afterResponse(HandshakeResponse hr) {
            if (hr.getHeaders().containsKey("ConfiguratorHeader")) {
                success = true;
            }
        }
    }

    @Override
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
    }

}
