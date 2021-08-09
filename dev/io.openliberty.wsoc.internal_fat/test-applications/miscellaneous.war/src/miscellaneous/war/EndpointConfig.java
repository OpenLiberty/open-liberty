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
package miscellaneous.war;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

public class EndpointConfig implements ServerEndpointConfig {

    ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
    ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

//    TextEndpoint
//    AsyncTextEndpoint
//    ReaderEndpoint
//    InputStreamEndpoint
//    ByteBufferEndpoint
//    ByteArrayEndpoint
//    PartialTextEndpoint
//    PartialBinaryEndpoint
//    onClose(Session, CloseReason)
//    onError(Session, Throwable)

    public static class PartialEndpointConfig extends EndpointConfig {

        @Override
        public String getPath() {
            return "/codedPartial";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgServerEPS.PartialEndpoint.class;
        }
    }

    public static class AsyncTextEndpointConfig extends EndpointConfig {

        @Override
        public String getPath() {
            return "/codedAsyncText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgServerEPS.AsyncTextEndpoint.class;
        }
    }

    public static class BasicEndpointConfig extends EndpointConfig {

        @Override
        public String getPath() {
            return "/codedBasic";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgServerEPS.BasicEndpoint.class;
        }
    }

    public static class Basic2Config extends EndpointConfig {

        @Override
        public String getPath() {
            return "/codedBasic2";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgServerEPS.Basic2Endpoint.class;
        }
    }

    public EndpointConfig() {
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
