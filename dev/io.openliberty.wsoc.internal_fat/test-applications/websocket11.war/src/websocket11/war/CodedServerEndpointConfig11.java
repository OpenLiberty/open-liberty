/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package websocket11.war;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

public class CodedServerEndpointConfig11 implements ServerEndpointConfig {

    ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
    ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

    public static class MessageHandlerWholeTextConfig extends CodedServerEndpointConfig11 {

        @Override
        public String getPath() {
            return "/codedText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return AddMessageHandlerEndpoint11.TextEndpoint.class;
        }
    }

    public static class MessageHandlerReaderConfig extends CodedServerEndpointConfig11 {

        @Override
        public String getPath() {
            return "/codedReader";
        }

        @Override
        public Class<?> getEndpointClass() {
            return AddMessageHandlerEndpoint11.ReaderEndpoint.class;
        }
    }

    public static class MessageHandlerInputStreamConfig extends CodedServerEndpointConfig11 {

        @Override
        public String getPath() {
            return "/codedInputStream";
        }

        @Override
        public Class<?> getEndpointClass() {
            return AddMessageHandlerEndpoint11.InputStreamEndpoint.class;
        }
    }

    public static class MessageHandlerPartialTextConfig extends CodedServerEndpointConfig11 {

        @Override
        public String getPath() {
            return "/codedPartialText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return AddMessageHandlerEndpoint11.PartialTextEndpoint.class;
        }
    }

    public static class MessageHandlerPartialSenderConfig extends CodedServerEndpointConfig11 {

        @Override
        public String getPath() {
            return "/codedPartialSenderText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return AddMessageHandlerEndpoint11.PartialSenderWholeReceiverEndpoint.class;
        }
    }

    public CodedServerEndpointConfig11() {
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
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.server.ServerEndpointConfig#getPath()
     */
    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

}
