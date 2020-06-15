/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war.configurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import basic.war.ProgrammaticServerEP;

public abstract class ConfiguratorEndpointConfig implements ServerEndpointConfig {

    ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
    ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

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

    public static class CodedModifyHandshakeEndpointConfig extends ConfiguratorEndpointConfig {

        @Override
        public String getPath() {
            return "/codedModifyHandshake";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.TextEndpoint.class;
        }

    }

    public static class FakeExtensionEndpointConfig extends ConfiguratorEndpointConfig {

        @Override
        public String getPath() {
            return "/codedExtension";
        }

        @Override
        public List<Extension> getExtensions() {
            List<Extension> extensionList = new ArrayList<Extension>(1);
            extensionList.add(new Extension() {

                @Override
                public String getName() {
                    return "TestExtension";
                }

                @Override
                public List<Parameter> getParameters() {
                    return Collections.emptyList();
                }

            });
            return extensionList;
        }

        @Override
        public Class<?> getEndpointClass() {
            return ExtensionServerEP.TextEndpoint.class;
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
