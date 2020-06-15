/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

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

    /**
     * 
     */
    public ProgrammaticConfiguratorClientEP() {
        // TODO Auto-generated constructor stub
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

    public static class ConfiguratorTest extends ProgrammaticConfiguratorClientEP {

        @Override
        public void onOpen(Session session, EndpointConfig epc) {

            ClientEndpointConfig cepc = (ClientEndpointConfig) epc;
            ClientConfigurator cc = (ClientConfigurator) cepc.getConfigurator();
            if (cc.success) {
                _wtr.overwriteSingleMessage("SUCCESS");
            }
            else {
                _wtr.overwriteSingleMessage("Failure, configurator did not modify request.");
            }
            _wtr.terminateClient();

        }

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
