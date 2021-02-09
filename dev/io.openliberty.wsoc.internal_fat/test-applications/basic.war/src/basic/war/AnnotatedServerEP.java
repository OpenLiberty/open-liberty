/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.Utils;

// import test.common.zos.ZFatNativeHelper;

/**
 *
 */
public class AnnotatedServerEP {

    @ServerEndpoint(value = "/annotatedByteArray/{boolean-var}")
    public static class ByteArrayTest extends AnnotatedServerEP {
        //test which shows boolean pair, session, @PathParam and actual message. Parameters can be in any order
        @OnMessage
        public byte[] echoText(boolean last, Session session, @PathParam("boolean-var") boolean booleanVar, byte[] data) { //session, msg and last can be at different param index
            if (session != null && last && booleanVar) {
                return data;
            } else {
                return null;
            }
        }

    }

    @ServerEndpoint(value = "/annotatedByteBuffer")
    public static class ByteBufferTest extends AnnotatedServerEP {

        //test which shows boolean pair, session, and actual message. Note params can be in any order. See previous test.
        @OnMessage
        public ByteBuffer echoText(ByteBuffer data, boolean last, Session session) { //session, msg and last can be at different param index
            if (session != null && last) {
                return data;
            } else {
                return null;
            }

        }
    }

    @ServerEndpoint(value = "/annotatedReader")
    public static class ReaderTest extends AnnotatedServerEP {
        @OnMessage
        public String echoText(Reader reader) {

            String retValue = "";
            try {
                retValue = Utils.getReaderText(reader);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retValue;

        }

    }

    @ServerEndpoint(value = "/annotatedText/{boolean-var}")
    public static class TextTest extends AnnotatedServerEP {
        //test which shows boolean pair, session, @PathParam and string type message. Parameters can be in any order
        @OnMessage
        public String echoText(Session session, String text, boolean last, @PathParam("boolean-var") boolean booleanVar) { //session, msg and last can be at different param index
            if (session != null && last && booleanVar) {
                return text;
            } else {
                return "FAILED";
            }
        }

    }

    // @ServerEndpoint(value = "/zannotatedText/{boolean-var}")
    // public static class ZosTextTest extends AnnotatedServerEP {
    //     //test which shows boolean pair, session, @PathParam and string type message. Parameters can be in any order
    //     @OnMessage
    //     public String echoText(Session session, String text, boolean last, @PathParam("boolean-var") boolean booleanVar) { //session, msg and last can be at different param index
    //         if (session != null && last && booleanVar) {
    //             byte[] currentEnclave = ZFatNativeHelper.getCurrentEnclave();
    //             //log("After call to getCurrentEnclave:" + currentEnclave);
    //             String currentTran = ZFatNativeHelper.getTransactionClass(currentEnclave);
    //             //log("After call to getCurrentTransactionClass:" + currentTran);

    //             return currentTran;
    //         }
    //         return "FAILED";

    //     }

    // }

    @ServerEndpoint(value = "/annotatedInputStream")
    public static class InputStreamTest extends AnnotatedServerEP {

        @OnMessage
        public byte[] echoInputStream(InputStream stream) {
            byte[] retValue = null;
            try {
                retValue = Utils.getInputStreamData(stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retValue;
        }

    }

    @ServerEndpoint(value = "/annotatedBoolean")
    public static class BooleanTest extends AnnotatedServerEP {

        @OnMessage
        public boolean echoInputStream(boolean val) {
            return val;
        }

    }

//    TODO - CURRENTLY DO NOT HANDLE PASSING IN SESSION....
//    @ServerEndpoint(value = "/annotatedOnMsgVoidReturn")
//    public class OnMsgVoidReturnTest extends AnnotatedEndpoint {
//
//        @OnMessage
//        public void echoInputStream(String val, Session sess) {
//            try {
//                System.out.println("HERE SHOULD BE SENDING BACK " + val);
//                _curSess.getBasicRemote().sendText(val);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
//
//    }

    @ServerEndpoint(value = "/annotatedDouble")
    public static class DoubleTest extends AnnotatedServerEP {
        @OnMessage
        public Double echo(Double val) {
            return val;
        }
    }

    @ServerEndpoint(value = "/annotatedFloat")
    public static class FloatTest extends AnnotatedServerEP {
        @OnMessage
        public Float echo(Float val) {
            return val;
        }
    }

    @ServerEndpoint(value = "/annotatedInteger")
    public static class IntegerTest extends AnnotatedServerEP {
        @OnMessage
        public Integer echo(Integer val) {
            return val;
        }
    }

    @ServerEndpoint(value = "/annotatedLong")
    public static class LongTest extends AnnotatedServerEP {
        @OnMessage
        public Long echo(Long val) {
            return val;
        }

    }

    @ServerEndpoint(value = "/annotatedShort")
    public static class ShortTest extends AnnotatedServerEP {
        @OnMessage
        public Short echo(Short val) {
            return val;
        }
    }

    @ServerEndpoint(value = "/annotatedshort")
    public static class ShortPrimitiveTest extends AnnotatedServerEP {
        @OnMessage
        public short echo(short val) {
            return val;
        }
    }

    @ServerEndpoint(value = "/annotatedbytereturn")
    public static class PrimitivebyteReturnTest extends AnnotatedServerEP {
        @OnMessage
        public byte echoText(String data) {
            return Byte.parseByte(data);
        }
    }

    @ServerEndpoint(value = "/annotatedbyte")
    public static class PrimiteByteTest extends AnnotatedServerEP {
        @OnMessage
        public String echoText(byte data) {
            String returnString = String.valueOf(data);
            return returnString;
        }
    }

    @ServerEndpoint(value = "/annotatedMaxBinaryMessage")
    public static class MaxBinaryMessageTest extends AnnotatedServerEP {

        @OnMessage(maxMessageSize = 4)
        public byte[] echoText(Session session, byte[] data) {
            return data;
        }

    }

    @ServerEndpoint(value = "/annotatedMaxTextMessage")
    public static class MaxTextMessageTest extends AnnotatedServerEP {

        @OnMessage(maxMessageSize = 4)
        public String echoText(Session session, String data) {
            return data;
        }

    }

    /**
    *
    */
    @ServerEndpoint(value = "/annotatedIdleTimeout")
    public static class SessionIdleTimeoutEndpoint {

        @OnOpen
        public void onOpen(final Session session) {
            if (session != null) {

                //set idle timeout as 15 seconds
                session.setMaxIdleTimeout(15000);
            }
            try {
                session.getBasicRemote().sendText(String.valueOf(session.getMaxIdleTimeout()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
