/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public class AnnotatedConfiguratorClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(AnnotatedConfiguratorClientEP.class.getName());

    @ClientEndpoint(configurator = ClientConfigurator.class)
    public static class ConfiguratorTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
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

    @ClientEndpoint(configurator = TCKClientConfigurator.class)
    public static class TCKConfiguratorTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            ClientEndpointConfig cepc = (ClientEndpointConfig) epc;
            TCKClientConfigurator cc = (TCKClientConfigurator) cepc.getConfigurator();
            if (cc.success) {
                _wtr.overwriteSingleMessage("SUCCESS");
            }
            else {
                _wtr.overwriteSingleMessage("Failure, configurator did modify request.");
            }
            _wtr.terminateClient();

        }

    }

    public static class TCKClientConfigurator extends ClientEndpointConfig.Configurator {
        public boolean success = true;

        @Override
        public void afterResponse(HandshakeResponse hr) {
            success = true;

            boolean contains = hr.getHeaders().containsKey("GetQueryStringConfigurator");
            LOG.info("ClientConfigurator contains: " + contains);
            if (contains) {
                success = false;
            }
        }
    }

    @ClientEndpoint(configurator = ClientConfigurator.class)
    public static class FailedHandshakeTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            _wtr.overwriteSingleMessage("NOT GOOD");
            _wtr.terminateClient();
        }

    }

    @ClientEndpoint
    public static class VerifyNoExtensionTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            List<Extension> exts = sess.getNegotiatedExtensions();
            _wtr.addMessage("CLIENTNEGOTIATED" + (exts==null ? 0 : exts.size()) );
        }

        @OnMessage
        public void onMessage(Session sess, String msg) {
            _wtr.addMessage(msg);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

    }

    @ClientEndpoint(subprotocols = { "Test3", "Test2", "Test1" })
    public static class SubprotocolTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {

        }

        @OnMessage
        public void onMessage(Session sess, String msg) {
            _wtr.addMessage(msg);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

    }

    @ClientEndpoint(subprotocols = { "   Test1", "Test2   ", "  Test3  " })
    public static class ConfiguredSubprotocolTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            _wtr.addMessage(sess.getNegotiatedSubprotocol());

        }

        @OnMessage
        public void onMessage(Session sess, String msg) {
            _wtr.addMessage(msg);
            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

    }

    @ClientEndpoint(configurator = CaseConfigurator.class)
    public static class CaseConfiguratorTest extends AnnotatedConfiguratorClientEP {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            ClientEndpointConfig cepc = (ClientEndpointConfig) epc;
            CaseConfigurator cc = (CaseConfigurator) cepc.getConfigurator();
            _wtr.addMessage(cc.msg);

        }

        @OnMessage
        public void onMessage(Session sess, String msg) {
            _wtr.addMessage(msg);
            _wtr.terminateClient();
        }

    }

    public static class CaseConfigurator extends ClientEndpointConfig.Configurator {
        public String msg = "SUCCESS";

        @Override
        public void afterResponse(HandshakeResponse response) {

            List<String> li = response.getHeaders().get("serverheader");
            if (li == null) {
                msg = "CLIENT: No header for for key serverheader";
                return;
            }
            else if (!li.get(0).equals("FIRST")) {
                msg = "CLIENT: servertheader does not contain FIRST but " + li.get(0);
                return;
            }

            response.getHeaders().put("SERVERHEADER", new ArrayList<String>(
                            Arrays.asList("SECOND")));

            li = response.getHeaders().get("SERVERHEADER");
            if (li == null) {
                msg = "CLIENT: After put, No header for for key serverheader";
                return;
            }
            else if (!li.get(0).equals("SECOND")) {
                msg = "CLIENT: After put, header serverheader does not contain SECOND but " + li.get(0);
                return;
            }

            li = response.getHeaders().get("x-POWERED-by");
            if (li == null) {
                msg = "CLIENT:  No header for for key x-POWERED-by";
                return;
            }
            else if (!li.get(0).equals("SUCCESS")) {
                msg = "CLIENT: key x-POWERED-by does not contain SUCCESS but " + li.get(0);
                return;
            }

        }

        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            // New header
            headers.put("clientheader", new ArrayList<String>(
                            Arrays.asList("FAILURE")));

            headers.put("clientHEAdeR", new ArrayList<String>(
                            Arrays.asList("FIRST")));

            List<String> li = headers.get("CLIENTHEADER");

            System.out.println("IT IS " + headers.getClass());
            if (li == null) {
                msg = "CLIENT: No header for for key clientheader";
                return;
            }
            else if (!li.get(0).equals("FIRST")) {
                msg = "CLIENT: header clientheader does not contain SUCCESS but " + li.get(0);
                return;
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
