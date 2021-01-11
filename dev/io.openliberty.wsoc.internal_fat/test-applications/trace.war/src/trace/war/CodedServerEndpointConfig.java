/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trace.war;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

// import basic.war.ProgrammaticServerEP;

public class CodedServerEndpointConfig implements ServerEndpointConfig {

    ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
    ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

    public static class CloseEndpointOnOpenConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedCloseOnOpen";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.CloseEndpointOnOpen.class;
        }
    }

    public static class CloseEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedClose";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.CloseEndpoint.class;
        }
    }

    public CodedServerEndpointConfig() {
        // no-arg constructor
        // myDecoderList.add(DecoderOne.class);
        // myEncoderList.add(EncoderOne.class);
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return myDecoderList;
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return myEncoderList;
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

    /*
     * (non-Javadoc)
     *
     * @see javax.websocket.server.ServerEndpointConfig#getEndpointClass()
     */
    @Override
    public Class<?> getEndpointClass() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.websocket.server.ServerEndpointConfig#getPath()
     */
    @Override
    public String getPath() {
        return null;
    }

}
