/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import basic.jar.PathParamOnOpenServerEP;

/*
 * Good test of using an annotated endpoint class in a serverendpointconfig... that returns null for many methods.
 */
public class ServletUpgradePathEndpointConfig implements ServerEndpointConfig {

    public ServletUpgradePathEndpointConfig() {}

    @Override
    public String getPath() {
        return "/pathparamonopentest/{String-var}/{Integer-var}";
    }

    @Override
    public Class<?> getEndpointClass() {

        Class<?> cl = PathParamOnOpenServerEP.class;
        return cl;
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
    public Configurator getConfigurator() {
        ServerEndpointConfig.Configurator x = new ServerEndpointConfig.Configurator();
        return x;
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
