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
package com.ibm.ws.wsoc.outbound;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.wsoc.AnnotatedEndpoint;
import com.ibm.ws.wsoc.EndpointHelper;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.SessionImpl;
import com.ibm.ws.wsoc.WebSocketVersionServiceManager;
import com.ibm.ws.wsoc.external.SessionExt;
import com.ibm.ws.wsoc.external.WebSocketContainerExt;
import com.ibm.ws.wsoc.external.WebSocketFactory;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;

/**
 *
 */
public class ClientConnector {

    private static final TraceComponent tc = Tr.register(ClientConnector.class);

    public Session connectAnnotatedClass(Object annotatedClass, URI path, WebSocketContainer wsc) throws DeploymentException, IOException {

        EndpointHelper eph = getAnnotatedClientEndpointClass(annotatedClass.getClass(), path.toString());
        EndpointConfig endpointConfig = getEndpointConfig(eph);

        AnnotatedEndpoint aep = new AnnotatedEndpoint();

        aep.initialize(annotatedClass.getClass(), endpointConfig, false);

        aep.setAppInstance(annotatedClass);
        return connectClass(aep, path, (ClientEndpointConfig) endpointConfig, wsc);

    }

    public Session connectClass(Object clazz, URI path, ClientEndpointConfig config, WebSocketContainer wsc) throws DeploymentException, IOException {

        WsocAddress endpointAddress = new WsocAddress(path);
        endpointAddress.validateURI();

        ParametersOfInterest things = new ParametersOfInterest();

        HttpRequestor requestor = new HttpRequestor(endpointAddress, config, things);
        WsByteBuffer remainingBuf = null;

        try {
            requestor.connect();
            requestor.sendRequest();
        } catch (InvalidChainNameException ice) {
            String msg = Tr.formatMessage(tc, "client.connection.nossl", endpointAddress.toString(), ice);
            throw new IOException(msg, ice);
        } catch (IOException e) {
            Tr.error(tc, "client.connection.error", endpointAddress.toString(), e.getMessage());
            String msg = Tr.formatMessage(tc, "client.connection.error", endpointAddress.toString(), e.getMessage());
            IOException up = new IOException(msg, e);
            requestor.closeConnection(up);
            throw up;
        } catch (Exception e) {
            Tr.error(tc, "client.connection.error", endpointAddress.toString(), e.getMessage());
            String msg = Tr.formatMessage(tc, "client.connection.error", endpointAddress.toString(), e.getMessage());
            throw new IOException(msg, e);
        }

        try {
            remainingBuf = requestor.completeResponse();
        } catch (IOException up) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "requestor.completeResponse threw IOException of: " + up);
            }
            requestor.closeConnection(up);
            throw up;
        }

        SessionImpl sessionImpl = new SessionImpl();

        WebSocketFactory webSocketFactory = WebSocketVersionServiceManager.getWebSocketFactory();
        SessionExt sessionExt = webSocketFactory.getWebSocketSession();

        sessionExt.initialize(sessionImpl);
        sessionImpl.initialize((Endpoint) clazz, config, requestor.getClientTransportAccess(), sessionExt, (WebSocketContainerExt) wsc, true);
        sessionImpl.setParametersOfInterest(things);

        ClassLoader cl = Utils.getContextClassloaderPrivileged();

        things.setTccl(cl);
        things.setCmd(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData());

        sessionImpl.signalAppOnOpen(remainingBuf, true);

        return sessionExt;
    }

    private ClientEndpointConfig getEndpointConfig(EndpointHelper helper) throws DeploymentException {

        Builder builder = ClientEndpointConfig.Builder.create();

        if (helper.getDecoders() != null) {
            builder.decoders(helper.getDecoders());
        }
        if (helper.getEncoders() != null) {
            builder.encoders(helper.getEncoders());
        }

        if (helper.getClientEndpointConfigurator() != null) {
            try {
                builder.configurator(helper.getClientEndpointConfigurator().newInstance());
            } catch (Exception in) {
                String msg = Tr.formatMessage(tc, "client.invalid.configurator", helper.getClientEndpointConfigurator().getClass(), in.getMessage());
                Tr.error(tc, "client.invalid.configurator", helper.getClientEndpointConfigurator().getClass(), in.getMessage());
                throw new DeploymentException(msg, in);
            }
        }

        if (helper.getSubprotocols() != null) {
            builder.preferredSubprotocols(Arrays.asList(helper.getSubprotocols()));
        }

        return builder.build();

    }

    private EndpointHelper getAnnotatedClientEndpointClass(Class<?> c, String contextPath) throws DeploymentException {

        ClientEndpoint ce = c.getAnnotation(ClientEndpoint.class);
        if (ce == null) {
            String msg = Tr.formatMessage(tc, "client.invalid.endpoint", c);
            Tr.error(tc, "client.invalid.endpoint", c);
            throw new DeploymentException(msg);
        }

        EndpointHelper helper = new EndpointHelper();

        helper.setEndpointClass(c);

        Class<? extends Decoder>[] decoders = ce.decoders();
        if ((decoders != null) && (decoders.length > 0)) {
            ArrayList<Class<? extends Decoder>> list = new ArrayList<Class<? extends Decoder>>();
            for (int i = 0; i < decoders.length; i++) {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "found decoder of: " + decoders[i]);
                }

                list.add(decoders[i]);
            }

            helper.setDecoders(list);
        }

        Class<? extends Encoder>[] encoders = ce.encoders();
        if ((encoders != null) && (encoders.length > 0)) {
            ArrayList<Class<? extends Encoder>> list = new ArrayList<Class<? extends Encoder>>();
            for (int i = 0; i < encoders.length; i++) {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "found encoder of: " + encoders[i]);
                }

                list.add(encoders[i]);
            }

            helper.setEncoders(list);
        }

        Class<? extends ClientEndpointConfig.Configurator> configurator = ce.configurator();
        if ((configurator != null)) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "found configurator of: " + configurator);
            }

            helper.setClientEndpointConfigurator(configurator);
        }
        String[] subprotocols = ce.subprotocols();
        if (subprotocols != null) {

            if (tc.isDebugEnabled()) {
                for (String protocol : subprotocols) {
                    Tr.debug(tc, "found a subprotocol " + protocol);
                }
            }

            helper.setSubprotocols(subprotocols);
        }

        return helper;
    }

}
