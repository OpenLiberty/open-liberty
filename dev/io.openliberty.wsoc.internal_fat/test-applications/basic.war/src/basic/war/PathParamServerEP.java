/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.Utils;

/**
 *
 */
public class PathParamServerEP {

    @ServerEndpoint(value = "/pathparamtest/{String-var}/{char-var}/{int-var}/{Integer-var}/{Long-var}/{long-var}/{Double-var}/{double-var}/{Short-var}/{short-var}")
    public static class TextTest extends PathParamServerEP {
        @OnMessage
        public String echoText(String text, @PathParam("String-var") String stringVar, @PathParam("char-var") char charVar, @PathParam("Integer-var") Integer integerVar,
                               @PathParam("int-var") int intVar) {
            String returnText = text + "," + stringVar + "," + charVar + "," + integerVar.toString() + "," + String.valueOf(intVar);
            return returnText;
        }
    }

    @ServerEndpoint(
                    value = "/pathparamtest/{String-var}/{Double-var}/{double-var}/{Short-var}/{short-var}/{Byte-var}/{byte-var}/{Boolean-var}/{boolean-var}/{Float-var}/{float-var}")
    public static class ReaderTest extends PathParamServerEP {
        @OnMessage
        public String echoText(Reader reader, @PathParam("Float-var") Float FloatVar, @PathParam("float-var") float floatVar, @PathParam("Short-var") Short ShortVar,
                               @PathParam("short-var") short shortVar, @PathParam("Double-var") Double DoubleVar,
                               @PathParam("double-var") double doubleVar, @PathParam("Byte-var") Byte ByteVar,
                               @PathParam("byte-var") byte byteVar, @PathParam("Boolean-var") Boolean BooleanVar, @PathParam("boolean-var") boolean booleanVar) {

            String retValue = "";
            try {
                retValue = Utils.getReaderText(reader) + "," + DoubleVar.toString() + "," + String.valueOf(doubleVar) + "," + ShortVar + "," + shortVar + "," + ByteVar + ","
                           + byteVar + "," + BooleanVar + "," + booleanVar + "," + FloatVar.floatValue() + "," + floatVar;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retValue;

        }
    }

    @ServerEndpoint(value = "/shorttest/{param}")
    public static class ShortTest extends PathParamServerEP {
        private Session _curSess = null;

        @OnMessage
        //show-cases 1) Short type of msg param 2) matching of path param name with {} brackets
        public void echoText(@PathParam("{param}") Short param, short msg) {
            String returnValue = new Short(msg).toString();
            try {
                _curSess.getBasicRemote().sendText(returnValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        @OnOpen
        public void onOpen(final Session session) {
            _curSess = session;
        }

        @OnError
        public void onError(Throwable error) {
            System.out.println("ShortTest.onError() called. Error message: " + error.getMessage());
        }
    }

    //SPec says: if @PathParam doesn't match any path segment in endpoint URI and if that @PathParam is of 
    //NON-String type,and if the pathparam is of any other type, then implementation should call @OnError 
    //method passing DecodeException since it cannot decode it.
    @ServerEndpoint(value = "/unmatchednonstring/{param1}")
    public static class UnmatchedNonStringPathParamTest {
        private Session _curSess = null;

        @OnMessage
        public String echoText(String text, @PathParam("param2") Boolean booleanVar) {
            return text;
        }

        @OnError
        public void onError(final Session session, Throwable error) {
            try {
                _curSess.getBasicRemote().sendText("Error");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException x) {

                }
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @OnOpen
        public void onOpen(final Session session, EndpointConfig ec) {
            _curSess = session;
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Spec says: if @PathParam doesn't match any path segment in endpoint URI and if that @PathParam 
    //is of NON-String type,and if the pathparam is of any other type, then implementation should 
    //call @OnError method passing DecodeException since it cannot decode it.
    @ServerEndpoint(value = "/unmatchedstring/{param3}")
    public static class UnmatchedStringPathParamTest {
        //{param1} does not match with any path segment in endpoint uri "/pathparamtest/{param}"
        @OnMessage
        public String echoText(String text, @PathParam("param4") String stringParam) {
            //implementation should have set the value of this stringParam to null
            if (stringParam == null)
                return "success";
            else
                return "Error";
        }
    }

    @ServerEndpoint(value = "/pathparamsessiontest/{guest-id}")
    public static class SessionPathParamTest extends PathParamServerEP {
        private static String pathparam;

        @OnMessage
        public String echoText(String text) {
            //return the pathparms obtained in onOpen()
            return pathparam;
        }

        @OnOpen
        public void onOpen(final Session session, EndpointConfig ec) {
            //if the session & EndpointConfig are declared, runtime should be passing them in
            if (session != null && ec != null) {
                String keys = Arrays.toString(session.getPathParameters().keySet().toArray());
                String values = Arrays.toString(session.getPathParameters().values().toArray());
                //store them to return it back on onMessage()
                pathparam = keys + values;
            }
        }
    }

    @ServerEndpoint(value = "/runtimeexceptionTCK/{param1}")
    public static class RuntimeExceptionTCKTest extends PathParamServerEP {
        private final String MSG = "TEST PUPRPOSELY THROWS RUNTIME EXCEPTION";

        @OnMessage
        public String echoText(String text) {
            System.out.println("In RuntimeExceptionTCKTest.echoText() " + MSG);
            throw new RuntimeException(MSG);
        }

        @OnError
        public void onError(final Session session, Throwable error, @PathParam("{param1}") long param) {
            try {
                //success case
                if (error.getMessage().equals(MSG)) {
                    session.getBasicRemote().sendText(Long.toString(param));
                } else { //failure case
                    //print into logs
                    error.printStackTrace();
                    session.getBasicRemote().sendText("FAILURE");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
