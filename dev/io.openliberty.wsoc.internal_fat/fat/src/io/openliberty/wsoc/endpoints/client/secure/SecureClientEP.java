/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.secure;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.endpoints.client.secure.SecureClientEP.SecureClientConfigurator;

/**
 *
 */
@ClientEndpoint(configurator = SecureClientConfigurator.class)
public class SecureClientEP implements TestHelper {

    public WsocTestContext _wtr = null;

    public int _counter = 0;

    static String user = "";
    static String password = "";

    public SecureClientEP(String user, String password) {
        SecureClientEP.user = user;
        SecureClientEP.password = password;
    }

    @OnOpen
    public void onOpen(Session sess) {
        try {
            sess.getBasicRemote().sendText("DATA");
        } catch (Exception e) {
            _wtr.addExceptionAndTerminate("Error publishing initial message", e);

        }
    }

    @OnClose
    public void onClose(CloseReason reason) {
        _wtr.addMessage(String.valueOf(reason.getCloseCode().getCode()));
        _wtr.terminateClient();
    }

    public static class SecureClientConfigurator extends ClientEndpointConfig.Configurator {
        public boolean success = true;

        @Override
        public void beforeRequest(Map<String, List<String>> headers) {

            ArrayList<String> al = new ArrayList<String>(1);
            try {
                al.add("Basic " + encode((user + ":" + password).getBytes("UTF-8")));

            } catch (UnsupportedEncodingException e) {
                // ignored
            }
            headers.put("Authorization", al);
        }

        private static String encode(byte[] bytes) {
            try {
                // Parse the base 64 string differently depending on JDK level because
                // on JDK 7/8 we have JAX-B, and on JDK 8+ we have java.util.Base64
                if (getMajorJavaVersion() < 8) {
                    // return DatatypeConverter.printBase64Binary(bytes)
                    Class<?> DatatypeConverter = Class.forName("javax.xml.bind.DatatypeConverter");
                    return (String) DatatypeConverter.getMethod("printBase64Binary", byte[].class).invoke(null, bytes);
                } else {
                    // return Base64.getEncoder().encodeToString(bytes);
                    Class<?> Base64 = Class.forName("java.util.Base64");
                    Object encodeObject = Base64.getMethod("getEncoder").invoke(null);
                    return (String) encodeObject.getClass().getMethod("encodeToString", byte[].class).invoke(encodeObject, bytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static int getMajorJavaVersion() {
            String version = System.getProperty("java.version");
            String[] versionElements = version.split("\\D");
            int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
            return Integer.valueOf(versionElements[i]);
        }
    }

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

}
