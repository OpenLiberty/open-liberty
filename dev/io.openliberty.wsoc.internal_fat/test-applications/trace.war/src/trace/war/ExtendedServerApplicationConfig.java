/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trace.war;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import trace.war.configurator.ConfiguratorEndpointConfig;

/**
 *
 */
public class ExtendedServerApplicationConfig implements ServerApplicationConfig {

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> arg0) {
        // we like them all
        return arg0;
    }

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> arg0) {
        Set<ServerEndpointConfig> configs = new HashSet<ServerEndpointConfig>();

        // Configurator endpoints
        configs.add(new ConfiguratorEndpointConfig.ExtendedModifyHandshakeEndpointConfig());
        return configs;
    }

}
