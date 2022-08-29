/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.Session;
import jakarta.websocket.MessageHandler;

import jakarta.websocket.ClientEndpointConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

import jakarta.websocket.ClientEndpointConfig;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.Extension;
import jakarta.websocket.Encoder;
import jakarta.websocket.Decoder;

import javax.net.ssl.SSLContext;

/**
 *  Contains everything for the testUserPropertiesOnClient test.
 *  Property List is contained within UserPropertyClientEndpointConfig 
 *  and actual Endpoint is UserPropertiesTest. 
 * 
 *  Test checks for properties to be passed from UserPropertyClientEndpointConfig to 
 *  the websocket session. 
 * 
 */
public abstract class ClientUserPropertiesClientEP extends Endpoint implements TestHelper {

    public WsocTestContext _wtr = null;

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

    public static class UserPropertiesTest extends ClientUserPropertiesClientEP {

        @Override
        public void onOpen(Session session, EndpointConfig epc) {

            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String text) {
                    //NO-OP
                }
            });
            
            Map<String, Object> userProperties = session.getUserProperties();
                
            if (userProperties.size() != 1) {
                throw new IllegalStateException("User properties map size differs. Expected: 1, Actual: " + userProperties.size());
            }
            
            if (!userProperties.containsKey("CLIENT-1"))
                throw new IllegalStateException("User properties map is missing entry with key [CLIENT-1]");

            userProperties.put("MODIFY-1", new Object());

            for (Map.Entry<String, Object> entry : userProperties.entrySet()) {
                _wtr.addMessage(entry.getKey());
            }

            _wtr.terminateClient();
        }
    }

    public static class UserPropertyClientEndpointConfig implements ClientEndpointConfig {

        private static final Map<String, Object> CLIENT_USER_PROPERTIES = new HashMap<>();

        static {
            CLIENT_USER_PROPERTIES.put("CLIENT-1", new Object());
        }

        public Map<String, Object> getUserProperties() {
            return CLIENT_USER_PROPERTIES;
        }

        public List<String> getPreferredSubprotocols() {
            return Collections.emptyList();
        }

        public List<Extension> getExtensions() {
            return Collections.emptyList();
        }

        public SSLContext getSSLContext() {
            return null;
        }

        public ClientEndpointConfig.Configurator getConfigurator() {
            return (ClientEndpointConfig.Configurator) new ClientUserPropertiesClientEP.UserPropertiesClientConfigurator();
        }

        public List<Class<? extends Encoder>> getEncoders() {
            return Collections.emptyList();
        }

        public List<Class<? extends Decoder>> getDecoders() {
            return Collections.emptyList();
        }
    }

    public static class UserPropertiesClientConfigurator extends ClientEndpointConfig.Configurator {

        public void afterResponse(HandshakeResponse hr) {

        }

        public void beforeRequest(Map<String, List<String>> headers) {

        }
    }

    @Override
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
    }

}
