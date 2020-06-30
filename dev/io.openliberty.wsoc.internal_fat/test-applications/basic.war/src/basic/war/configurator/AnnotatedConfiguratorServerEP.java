/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war.configurator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
            response.getHeaders().put("ConfiguratorHeader", new ArrayList<String>(
                            Arrays.asList("SUCCESS")));

            // Existing header
            response.getHeaders().put("X-Powered-By", new ArrayList<String>(
                            Arrays.asList("ONE", "TWO", "THREE")));

        }
    }

    @ServerEndpoint(value = "/annotatedTCKModifyHandshake", configurator = TCKServerConfigurator.class)
    public static class TCKConfiguratorTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {

        }

    }

    public static class TCKServerConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec,
                                    HandshakeRequest request,
                                    HandshakeResponse response) {

            String KEY = "GetQueryStringConfigurator";
            String[] VALUES = { "ReadOnlyValue1", "ReadOnlyValue2" };

            Map<String, List<String>> map = request.getHeaders();
            System.out.println("BEFORE KEYS" + Arrays.toString(map.keySet().toArray()));
            System.out.println("BEFORE VALUES" + Arrays.toString(map.values().toArray()));
            try {
                map.put(KEY, Arrays.asList(VALUES));
            } catch (Exception e) {
                //e.printStackTrace();
            }

            map = request.getHeaders();

            System.out.println("KEYS" + Arrays.toString(map.keySet().toArray()));
            System.out.println("VALUES" + Arrays.toString(map.values().toArray()));

            response.getHeaders().putAll(map);

            System.out.println("AFTER KEYS" + Arrays.toString(map.keySet().toArray()));
            System.out.println("AFTER VALUES" + Arrays.toString(map.values().toArray()));
        }
    }

    @ServerEndpoint(value = "/annotatedFailHandshake", configurator = FailHandshakeConfigurator.class)
    public static class FailHandshakeTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {

        }

    }

    public static class FailHandshakeConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec,
                                    HandshakeRequest request,
                                    HandshakeResponse response) {

            //This also works.. but no test for this.
            //response.getHeaders().put("Sec-WebSocket-Accept", null);

            response.getHeaders().put("Sec-WebSocket-Accept", new ArrayList<String>() {});
        }
    }

    @ServerEndpoint(value = "/annotatedSubprotocol", subprotocols = { "Test1", "Test2", "Test3" })
    public static class SubprotocolTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            try {

                sess.getBasicRemote().sendText(sess.getNegotiatedSubprotocol());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @ServerEndpoint(value = "/annotatedModifySubprotocol", subprotocols = { "Test1", "Test2", "Test3" }, configurator = SubprotocolConfigurator.class)
    public static class ModifySubprotocolTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            try {

                ServerEndpointConfig sec = (ServerEndpointConfig) epc;
                SubprotocolConfigurator spc = (SubprotocolConfigurator) sec.getConfigurator();
                sess.getBasicRemote().sendText(String.valueOf(spc.supportedCorrect));
                sess.getBasicRemote().sendText(String.valueOf(spc.requestedCorrect));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    @ServerEndpoint(value = "/annotatedCheckOrigin", configurator = OriginConfigurator.class)
    public static class CheckOriginTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {

        }

    }

    public static class OriginConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public boolean checkOrigin(String originHeader) {
            return false;
        }
    }

    public static class SubprotocolConfigurator extends ServerEndpointConfig.Configurator {
        public boolean supportedCorrect = true;
        public boolean requestedCorrect = true;

        @Override
        public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {

            if (supported.size() != 3) {
                supportedCorrect = false;
            }
            else {
                if ((!supported.get(0).equals("Test1")) && (!supported.get(1).equals("Test2")) || (!supported.get(2).equals("Test3"))) {
                    supportedCorrect = false;
                }
            }
            if (requested.size() != 3) {
                requestedCorrect = false;
            }
            else {
                if ((!requested.get(0).equals("Test1")) || (!requested.get(1).equals("Test2")) || (!requested.get(2).equals("Test3"))) {
                    requestedCorrect = false;
                }
            }

            // We'll send back Test2 instead of Test1
            return "Test2";
        }
    }

    @ServerEndpoint(value = "/annotatedCaseConfigurator", configurator = ServerCaseConfigurator.class)
    public static class CaseConfiguratorTest {

        @OnOpen
        public void onOpen(Session sess, EndpointConfig epc) {
            ServerEndpointConfig cepc = (ServerEndpointConfig) epc;
            ServerCaseConfigurator cc = (ServerCaseConfigurator) cepc.getConfigurator();
            try {
                sess.getBasicRemote().sendText(cc.msg);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public static class ServerCaseConfigurator extends ServerEndpointConfig.Configurator {
        String msg = "SUCCESS";

        @Override
        public void modifyHandshake(ServerEndpointConfig sec,
                                    HandshakeRequest request,
                                    HandshakeResponse response) {

            List<String> li = request.getHeaders().get("clientheader");
            if (li == null) {
                msg = "SERVER: No header for for key clienttest";
                return;
            }
            else if (!li.get(0).equals("FIRST")) {
                msg = "SERVER: clientheader does not contain SUCCESS but " + li.get(0);
                return;
            }

            request.getHeaders().put("CLIENTHEADER", new ArrayList<String>(
                            Arrays.asList("SECOND")));

            li = request.getHeaders().get("cLieNtHeADer");
            if (li == null) {
                msg = "SERVER: After put, No header for for key clientheader";
                return;
            }
            else if (!li.get(0).equals("SECOND")) {
                msg = "SERVER: After put, header clientheader does not contain SUCCESS but " + li.get(0);
                return;
            }

            // New header
            response.getHeaders().put("serverheader", new ArrayList<String>(
                            Arrays.asList("FAILURE")));

            response.getHeaders().put("ServerHEAdeR", new ArrayList<String>(
                            Arrays.asList("FIRST")));

            li = response.getHeaders().get("SERVERHEADER");
            if (li == null) {
                msg = "SERVER: No header for for key serverheader";
                return;
            }
            else if (!li.get(0).equals("FIRST")) {
                msg = "SERVER: header SERVERHEADER does not contain SUCCESS but " + li.get(0);
                return;
            }

            // Existing header
            response.getHeaders().put("x-powered-by", new ArrayList<String>(
                            Arrays.asList("SUCCESS")));

        }
    }

}
