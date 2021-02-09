/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import basic.war.configurator.ConfiguratorEndpointConfig;

public class ExtendedServerApplicationConfig implements ServerApplicationConfig {

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> arg0) {
        // we like them all
        return arg0;
    }

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> arg0) {
        Set<ServerEndpointConfig> configs = new HashSet<ServerEndpointConfig>();

        configs.add(new ExtendedServerEndpointConfig.CodingEndpointConfig());
        configs.add(new ExtendedServerEndpointConfig.ByteArrayEndpointConfig());
        configs.add(new ExtendedServerEndpointConfig.ByteBufferEndpointConfig());
        configs.add(new ExtendedServerEndpointConfig.InputStreamEndpointConfig());
        configs.add(new ExtendedServerEndpointConfig.ReaderEndpointConfig());
        configs.add(new ExtendedServerEndpointConfig.TextEndpointConfig());
        configs.add(new ExtendedServerEndpointConfig.AsyncTextEndpointConfig());

        // Configurator endpoints
        configs.add(new ConfiguratorEndpointConfig.ExtendedModifyHandshakeEndpointConfig());
        configs.add(new ConfiguratorEndpointConfig.FakeExtensionEndpointConfig());

        return configs;
    }

}
