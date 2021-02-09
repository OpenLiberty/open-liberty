/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;
import javax.websocket.server.ServerEndpointConfig;

import io.openliberty.wsoc.common.ExtensionExt;
import io.openliberty.wsoc.common.ParameterExt;

import basic.war.coding.DecoderOne;
import basic.war.coding.EncoderOne;

public class CodedServerEndpointConfig implements ServerEndpointConfig {

    ArrayList<Class<? extends Decoder>> myDecoderList = new ArrayList<Class<? extends Decoder>>();
    ArrayList<Class<? extends Encoder>> myEncoderList = new ArrayList<Class<? extends Encoder>>();

    public static class AsyncTextEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedAsyncText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.TextEndpoint.class;
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

    public static class OnErrorEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedOnError";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.OnErrorEndpoint.class;
        }
    }

    public static class TextEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.TextEndpoint.class;
        }

        @Override
        public List<Extension> getExtensions() {
            Parameter p1 = new ParameterExt("the", "value");
            Parameter p2 = new ParameterExt("SECOND", "SECOND");
            Parameter p3 = new ParameterExt("third", "12345678");
            Extension e1 = new ExtensionExt("FirstExt", Arrays.asList(p1, p2));
            Extension e2 = new ExtensionExt("SecondExt", Arrays.asList(p3));

            return Arrays.asList(e1, e2);
        }
    }

    public static class PartialTextEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedPartialText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.PartialTextEndpoint.class;
        }
    }

    public static class PartialTextSenderEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedPartialSenderText";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.PartialSenderWholeReceiverEndpoint.class;
        }
    }

    public static class PartialTextEndpointConfig2 extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedPartialText2";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.PartialTextEndpoint2.class;
        }
    }

    public static class PartialTextWithSendingEmbeddedPingEndpoint extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/PartialTextWithSendingEmbeddedPingEndpoint";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.PartialTextWithSendingEmbeddedPingEndpoint.class;
        }
    }

    public static class ReaderEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedReader";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.ReaderEndpoint.class;
        }
    }

    public static class InputStreamEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedInputStream";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.InputStreamEndpoint.class;
        }
    }

    public static class ByteBufferEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedByteBuffer";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.ByteBufferEndpoint.class;
        }
    }

    public static class ByteArrayEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedByteArray";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.ByteArrayEndpoint.class;
        }
    }

    //  MSN TODO - This will need checked when we get back in when support for multiple server MessageHandlers is in place.
    public static class PingPongEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedPingPong";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.PingPongEndpoint.class;
        }
    }

    public static class CodingEndpointConfig extends CodedServerEndpointConfig {

        public CodingEndpointConfig() {
            myDecoderList.add(DecoderOne.class);
            myEncoderList.add(EncoderOne.class);
        }

        @Override
        public String getPath() {
            return "/codedCoding";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.CodingEndpoint.class;
        }

        @Override
        public List<Class<? extends Decoder>> getDecoders() {
            return myDecoderList;
        }

        @Override
        public List<Class<? extends Encoder>> getEncoders() {
            return myEncoderList;
        }
    }

    public static class MsgHandlerInheritanceConfig extends CodedServerEndpointConfig {

        public MsgHandlerInheritanceConfig() {
            myDecoderList.add(DecoderOne.class);
            myEncoderList.add(EncoderOne.class);
        }

        @Override
        public String getPath() {
            return "/msgHandlerInheritance";
        }

        @Override
        public Class<?> getEndpointClass() {
            return MsgHandlerInheritanceServerEP.class;
        }

        @Override
        public List<Class<? extends Decoder>> getDecoders() {
            return myDecoderList;
        }

        @Override
        public List<Class<? extends Encoder>> getEncoders() {
            return myEncoderList;
        }
    }

    public static class TextPathParamEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/sessionpathaparam/{guest-id}";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.SessionPathParamEndpoint.class;
        }
    }

    public static class ProgrammaticMaxMessageSizeConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/programmaticMaxMessageSize";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.MaxMessageSizeInSession.class;
        }
    }

    public static class TextQueryParmsEndpointConfig extends CodedServerEndpointConfig {

        @Override
        public String getPath() {
            return "/codedTextQueryParms";
        }

        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticServerEP.TextEndpointQueryParms.class;
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
