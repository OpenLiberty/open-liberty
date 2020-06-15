/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
