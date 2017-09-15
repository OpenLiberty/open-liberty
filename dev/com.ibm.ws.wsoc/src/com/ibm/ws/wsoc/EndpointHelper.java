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
package com.ibm.ws.wsoc;

import java.util.List;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.server.ServerEndpointConfig;

/**
 *
 */
public class EndpointHelper {

    String invocationPath = null;
    public List<Class<? extends Decoder>> decoders = null;
    public List<Class<? extends Encoder>> encoders = null;
    public Class<? extends ServerEndpointConfig.Configurator> serverEndpointConfigurator = null;
    public Class<? extends ClientEndpointConfig.Configurator> clientEndpointConfigurator = null;
    private String subProtocols[] = null;

    Class<?> endpointClass = null;
    ServerEndpointConfig serverEndpointConfig = null;

    public String getInvocationPath() {
        return invocationPath;
    }

    public void setInvocationPath(String _value) {
        invocationPath = _value;
    }

    public List<Class<? extends Decoder>> getDecoders() {
        return decoders;
    }

    public void setDecoders(List<Class<? extends Decoder>> _decoders) {
        decoders = _decoders;
    }

    public List<Class<? extends Encoder>> getEncoders() {
        return encoders;
    }

    public void setEncoders(List<Class<? extends Encoder>> _encoders) {
        encoders = _encoders;
    }

    public Class<?> getEndpointClass() {
        return endpointClass;
    }

    public void setEndpointClass(Class<?> _endpointClass) {
        endpointClass = _endpointClass;
    }

    public ServerEndpointConfig getServerEndpointConfig() {
        return serverEndpointConfig;
    }

    public void setServerEndpointConfig(ServerEndpointConfig config) {
        serverEndpointConfig = config;
    }

    public Class<? extends ServerEndpointConfig.Configurator> getServerEndpointConfigurator() {
        return serverEndpointConfigurator;
    }

    public void setServerEndpointConfigurator(Class<? extends ServerEndpointConfig.Configurator> configurator) {
        serverEndpointConfigurator = configurator;
    }

    public Class<? extends ClientEndpointConfig.Configurator> getClientEndpointConfigurator() {
        return clientEndpointConfigurator;
    }

    public void setClientEndpointConfigurator(Class<? extends ClientEndpointConfig.Configurator> configurator) {
        clientEndpointConfigurator = configurator;
    }

    public String[] getSubprotocols() {
        return subProtocols;
    }

    public void setSubprotocols(String[] prots) {
        subProtocols = prots;
    }

}
