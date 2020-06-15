/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2019 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.trace;

import java.util.List;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public class AnnotatedConfiguratorClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    // private static final Logger LOG = Logger.getLogger(AnnotatedConfiguratorClientEP.class.getName());

    @ClientEndpoint(configurator = ClientConfigurator.class)
    public static class ConfiguratorTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
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

    public static class ClientConfigurator extends ClientEndpointConfig.Configurator {
        public boolean success = true;

        @Override
        public void afterResponse(HandshakeResponse hr) {
            success = true;

            // Added header
            if (!hr.getHeaders().containsKey("ConfiguratorHeader")) {
                success = false;
            }
            //modified multiple header
            List<String> headers = hr.getHeaders().get("X-Powered-By");
            if ((!headers.get(0).equals("ONE")) || (!headers.get(1).equals("TWO")) || (!headers.get(2).equals("THREE"))) {
                success = false;
            }

        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

    }

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
    }

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

}
