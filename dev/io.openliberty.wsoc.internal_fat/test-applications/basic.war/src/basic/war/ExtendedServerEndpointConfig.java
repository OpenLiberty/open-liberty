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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import basic.war.coding.DecoderOne;
import basic.war.coding.EncoderOne;

public abstract class ExtendedServerEndpointConfig implements ServerEndpointConfig {

    ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
    ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

    public static class AsyncTextEndpointConfig extends ExtendedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedAsyncText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.AsyncTextEndpoint.class;
        }
    }

    public static class TextEndpointConfig extends ExtendedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.TextEndpoint.class;
        }
    }

    public static class ReaderEndpointConfig extends ExtendedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedReader";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.ReaderEndpoint.class;
        }
    }

    public static class InputStreamEndpointConfig extends ExtendedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedInputStream";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.InputStreamEndpoint.class;
        }
    }

    public static class ByteBufferEndpointConfig extends ExtendedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedByteBuffer";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.ByteBufferEndpoint.class;
        }
    }

    public static class ByteArrayEndpointConfig extends ExtendedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/extendedByteArray";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.ByteArrayEndpoint.class;
        }
    }

    public static class CodingEndpointConfig extends CodedServerEndpointConfig {

        ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
        ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

        @Override
        public String getPath() {
            return "/extendedCoding";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.CodingEndpoint.class;
        }

        @Override
        public List<Class<? extends Decoder>> getDecoders() {
            List<Class<? extends Decoder>> decs = new ArrayList<Class<? extends Decoder>>();
            decs.add(DecoderOne.class);
            return decs;
        }

        @Override
        public List<Class<? extends Encoder>> getEncoders() {
            List<Class<? extends Encoder>> encs = new ArrayList<Class<? extends Encoder>>();
            encs.add(EncoderOne.class);
            return encs;
        }

    }

    public ExtendedServerEndpointConfig() {
        // no-arg constructor
        // myDecoderList.add(DecoderOne.class);
        //  myEncoderList.add(EncoderOne.class);
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
