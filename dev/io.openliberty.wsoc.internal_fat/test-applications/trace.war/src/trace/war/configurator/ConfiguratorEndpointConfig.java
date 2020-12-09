/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trace.war.configurator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import trace.war.ProgrammaticServerEP;

/**
 *
 */
public abstract class ConfiguratorEndpointConfig implements ServerEndpointConfig {

    public static class ExtendedModifyHandshakeEndpointConfig extends ConfiguratorEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedModifyHandshake";

        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.TextEndpoint.class;
        }

    }

    @Override
    public Configurator getConfigurator() {

        return new ServerEndpointConfig.Configurator() {
            @Override
            public void modifyHandshake(ServerEndpointConfig sec,
                                        HandshakeRequest request,
                                        HandshakeResponse response) {
                List<String> addList = new ArrayList<String>(1);
                addList.add("SUCCESS");
                response.getHeaders().put("ConfiguratorHeader", addList);

            }
        };
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return null;
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return null;
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return null;
    }

    @Override
    public List<Extension> getExtensions() {
        return null;
    }

    @Override
    public List<String> getSubprotocols() {
        return null;
    }

}
