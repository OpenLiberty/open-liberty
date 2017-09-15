/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.wsoc.external.ExtensionExt;
import com.ibm.ws.wsoc.external.HandshakeRequestExt;
import com.ibm.ws.wsoc.external.HandshakeResponseExt;
import com.ibm.ws.wsoc.external.ParameterExt;
import com.ibm.ws.wsoc.util.Utils;

/* example of request and response
 GET /chat HTTP/1.1
 Host: server.example.com
 Upgrade: websocket
 Connection: Upgrade
 Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
 Origin: http://example.com
 Sec-WebSocket-Protocol: chat, superchat
 Sec-WebSocket-Version: 13

 Server responds like this:

 HTTP/1.1 101 Switching Protocols
 Upgrade: websocket
 Connection: Upgrade
 Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
 */

public class HandshakeProcessor {

    private static final TraceComponent tc = Tr.register(HandshakeProcessor.class);

    private String headerHost = null;
    private String headerUpgrade = null;
    private String headerConnection = null;
    private String headerSecWebSocketKey = null;
    private String headerOrigin = null;
    private String headerSecWebSocketProtocol = null;
    private String headerSecWebSocketVersion = null;

    private String[] clientSubProtocols = null;
    private List<Extension> clientExtensions = null;
    private List<Extension> configuredExtensions = null;

    private final static String subProtocolDelimiter = ",";

    // stuff used for populating the HandshakeRequest object for (user) configurator modification
    private final Map<String, List<String>> requestHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();
    private URI requestURI = null;

    private HttpServletResponse httpResponse = null;
    private HttpServletRequest httpRequest = null;
    private Map<String, String> extraParamMap = null;

    private ServerEndpointConfig endpointConfig = null;
    private ServerEndpointConfig.Configurator endpointConfigurator = null;

    private final ParametersOfInterest things = new ParametersOfInterest();

    public HandshakeProcessor() {}

    public void initialize(HttpServletRequest _hsr, HttpServletResponse resp, Map<String, String> extraParams) {
        httpRequest = _hsr;
        httpResponse = resp;
        extraParamMap = extraParams;
    }

    public void addWsocConfigurationData(ServerEndpointConfig _config, ServerEndpointConfig.Configurator _configurator) {
        endpointConfig = _config;
        endpointConfigurator = _configurator;
    }

    public ParametersOfInterest getParametersOfInterest() {
        return things;
    }

    public void readRequestInfo() throws Exception {

        Enumeration<String> names = httpRequest.getHeaderNames();

        Map<String, String[]> paramMap = httpRequest.getParameterMap();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            String[] value = entry.getValue();
            List<String> list = Arrays.asList(value);
            parameterMap.put(key, list);
        }

        if (extraParamMap != null) {
            for (String key : extraParamMap.keySet()) {
                parameterMap.put(key, Arrays.asList(extraParamMap.get(key)));
            }
        }

        requestURI = new URI(httpRequest.getRequestURI());

        things.setParameterMap(parameterMap);
        things.setQueryString(httpRequest.getQueryString());
        things.setURI(requestURI);
        things.setUserPrincipal(httpRequest.getUserPrincipal());
        things.setSecure(httpRequest.isSecure());
        things.setHttpSession(httpRequest.getSession());

        while (names.hasMoreElements()) {
            String name = names.nextElement();

            Enumeration<String> headerValues = httpRequest.getHeaders(name);
            List<String> list = Collections.list(headerValues);
            requestHeaders.put(name, list);

            String headerValue = httpRequest.getHeader(name);
            String compName = name.trim();

            if (compName.compareToIgnoreCase(Constants.HEADER_NAME_HOST) == 0) {
                headerHost = headerValue;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "host header has a value of:  " + headerHost);
                }

            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_UPGRADE) == 0) {
                headerUpgrade = headerValue;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "upgrade header has a value of:  " + headerUpgrade);
                }

            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_CONNECTION) == 0) {
                headerConnection = headerValue;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "connection header has a value of:  " + headerConnection);
                }

            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_KEY) == 0) {
                headerSecWebSocketKey = headerValue;
                if (headerSecWebSocketKey != null) {
                    headerSecWebSocketKey = headerSecWebSocketKey.trim();
                }

            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_ORIGIN) == 0) {
                headerOrigin = headerValue;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Origin header has a value of:  " + headerOrigin);
                }

            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_PROTOCOL) == 0) {
                for (String val : list) {
                    if (headerSecWebSocketProtocol == null) {
                        headerSecWebSocketProtocol = val;
                    }
                    else {
                        headerSecWebSocketProtocol = headerSecWebSocketProtocol + subProtocolDelimiter + val;
                    }
                }
                // parse into array for later comparisons
                if ((headerSecWebSocketProtocol != null) && (headerSecWebSocketProtocol.length() > 0)) {
                    clientSubProtocols = headerSecWebSocketProtocol.split(subProtocolDelimiter);

                    // trim out the white spaces
                    int len = clientSubProtocols.length;
                    for (int i = 0; i < len; i++) {
                        clientSubProtocols[i] = clientSubProtocols[i].trim();
                    }
                }
            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS) == 0) {
                clientExtensions = parseClientExtensions(list);

            } else if (compName.compareToIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION) == 0) {
                headerSecWebSocketVersion = headerValue;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "WebSocketVersion header has a value of:  " + headerSecWebSocketVersion);
                }
                things.setWsocProtocolVersion(headerSecWebSocketVersion);
            }
        }

    }

    public void addResponseHeaders() {

        // set the following headers in the HTTP Response object
        // upgrade, connection, Sec-WebSocket-Accept
        String accept;

        httpResponse.setHeader(Constants.HEADER_NAME_UPGRADE, Constants.HEADER_VALUE_WEBSOCKET);
        httpResponse.setHeader(Constants.HEADER_NAME_CONNECTION, Constants.HEADER_VALUE_UPGRADE);
        accept = this.makeAcceptResponseHeaderValue();

        httpResponse.setHeader(Constants.MC_HEADER_NAME_SEC_WEBSOCKET_ACCEPT, accept);

    }

    public boolean checkOrigin() {
        return endpointConfigurator.checkOrigin(headerOrigin);
    }

    public void modifyHandshake() {

        Map<String, List<String>> responseHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

        Collection<String> names = httpResponse.getHeaderNames();
        Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            String name = it.next();
            Collection<String> headerValues = httpResponse.getHeaders(name);
            List<String> list = new ArrayList<String>(headerValues);
            responseHeaders.put(name, list);
        }

        // create handshake request object to give to modifier
        HandshakeRequestExt handshakeRequest = new HandshakeRequestExt(httpRequest, requestHeaders, parameterMap, requestURI);

        // create handshake response object to give to modifier
        HandshakeResponseExt handshakeResponse = new HandshakeResponseExt(responseHeaders);

        endpointConfigurator.modifyHandshake(endpointConfig, handshakeRequest, handshakeResponse);
        Map<String, List<String>> hdrs = handshakeResponse.getHeaders();

        //  We can add new headers, or overwrite existing, but we cannot remove....
        // Iterator<String> i = hdrs.keySet().iterator();
        Iterator<Entry<String, List<String>>> i = hdrs.entrySet().iterator();
        while (i.hasNext()) {
            Entry<String, List<String>> entry = i.next();
            List<String> list = entry.getValue();

            if (list == null) {
                httpResponse.setHeader(entry.getKey(), "");
                continue;
            }
            if (list.size() == 0) {
                httpResponse.setHeader(entry.getKey(), "");
            }
            for (int x = 0; x < list.size(); x++) {
                if (x == 0) {
                    httpResponse.setHeader(entry.getKey(), list.get(x));
                }
                else {
                    httpResponse.addHeader(entry.getKey(), list.get(x));
                }
            }
        }
    }

    public void determineAndSetSubProtocol() {

        // the agree sub protocol is the first one in the local list the matches any of the ones in the client list.
        List<String> localSubProtocolList = endpointConfig.getSubprotocols();
        if (localSubProtocolList == null) {
            localSubProtocolList = Collections.emptyList();
        }

        List<String> clientProtocols = null;
        if (clientSubProtocols == null) {
            clientProtocols = Collections.emptyList();
        }
        else {
            clientProtocols = Arrays.asList(clientSubProtocols);
        }

        String agreedSubProtocol = endpointConfigurator.getNegotiatedSubprotocol(localSubProtocolList, clientProtocols);

        things.setAgreedSubProtocol(agreedSubProtocol);
        things.setLocalSubProtocols(localSubProtocolList);
        if ((!"".equals(agreedSubProtocol)) && (agreedSubProtocol != null)) {
            httpResponse.setHeader(Constants.HEADER_NAME_SEC_WEBSOCKET_PROTOCOL, agreedSubProtocol);
        }

    }

    public void determineAndSetExtensions() {

        // Even though we have no built in extensions, we will read the ServerEndpoingConfig to see what they configure
        //  COudl be an extension that looks at headers...
        configuredExtensions = endpointConfig.getExtensions();
        if (configuredExtensions == null) {
            configuredExtensions = Collections.emptyList();
        }

        if (clientExtensions == null) {
            clientExtensions = Collections.emptyList();
        }

        List<Extension> agreedExtensions = endpointConfigurator.getNegotiatedExtensions(configuredExtensions, clientExtensions);
        if (agreedExtensions != null) {
            if (agreedExtensions.size() > 0) {
                StringBuffer buf = new StringBuffer();
                boolean first = true;
                for (Extension ext : agreedExtensions) {
                    if (first) {
                        first = false;
                    }
                    else {
                        buf.append(", ");
                    }
                    buf.append(ext.getName());
                    List<Parameter> li = ext.getParameters();
                    if (li != null) {
                        if (li.size() > 0) {
                            for (Parameter p : li) {
                                buf.append("; " + p.getName() + "=" + p.getValue());
                            }
                        }
                    }

                }
                things.setNegotiatedExtensions(agreedExtensions);
                httpResponse.setHeader(Constants.HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS, buf.toString());
            }
        }
    }

    public void verifyHeaders() throws Exception {

        // Require HTTP/1.1 or above...
        String protocol = httpRequest.getProtocol();
        String[] args = protocol.split("/");
        if (args.length < 2) {
            throw new Exception("Websocket request processed, but no HTTP Protocol version was provided..");
        }
        else {
            float version = Float.valueOf(args[1]);
            if (version < 1.1) {
                throw new Exception("Websocket request processed, provided HTTP Protocol level \"" + protocol + "\"provided but WebSocket support requires HTTP/1.1 or above.");
            }
        }

        // Must have host header..
        if (headerHost == null) {
            throw new Exception("Websocket request processing found no Host header.");
        }

        // Must have Upgrade header equal to websocket
        if ((headerUpgrade == null) || (!Constants.HEADER_VALUE_WEBSOCKET.equalsIgnoreCase(headerUpgrade))) {
            throw new Exception("Websocket request processed but provided upgrade header \"" + headerUpgrade + "\" does not match " + Constants.HEADER_VALUE_WEBSOCKET
                                + ".");
        }

        //Must have  Connection:Upgrade header
        if (headerConnection != null) {
            boolean found = false;
            String[] connection = headerConnection.split(",");
            for (String conn : connection) {
                if (conn.trim().equalsIgnoreCase(Constants.HEADER_VALUE_UPGRADE)) {
                    found = true;
                }
            }
            if (!found) {
                throw new Exception("Websocket request processed but provided connection header \"" + headerConnection + "\" does not match " + Constants.HEADER_VALUE_UPGRADE
                                    + ".");
            }
        }
        else {
            throw new Exception("Websocket request processed but provided connection header \"" + headerConnection + "\" does not match " + Constants.HEADER_VALUE_UPGRADE
                                + ".");
        }

        // Version must equal 13... better logic will be provided once we support more than one version
        int supportedVersion = Integer.valueOf(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION);
        if (headerSecWebSocketVersion == null) {
            httpResponse.setIntHeader(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION, supportedVersion);
            throw new Exception("Websocket request processed, but no websocket version provided.");
        }
        else {
            try
            {
                int version = Integer.parseInt(headerSecWebSocketVersion);
                if (version != supportedVersion) {
                    httpResponse.setIntHeader(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION, supportedVersion);
                    throw new Exception("Websocket request processed, but Version header of \"" + headerSecWebSocketVersion + "\" is not a match for supported version "
                                        + supportedVersion + ".");
                }
            } catch (NumberFormatException nfe)
            {
                httpResponse.setIntHeader(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION, supportedVersion);
                throw new Exception("Websocket request processed, but version header of \"" + headerSecWebSocketVersion + "\" is not valid number.", nfe);
            }
        }

        // Check for blank or null accept key
        if (("".equals(headerSecWebSocketKey)) || (headerSecWebSocketKey == null)) {
            throw new Exception("Websocket request processed, but Sec-WebSocket-Key is blank or null");
        }
        byte[] decodedBytes = Base64Coder.base64DecodeString(headerSecWebSocketKey);

        if (decodedBytes == null) {
            throw new Exception("Websocket request processed, but client sent Sec-WebSocket-Key that has not been base64 encoded.");
        }
        if (decodedBytes.length != 16) {
            throw new Exception("Websocket request processed, but client sent Sec-WebSocket-Key that is not 16 bytes");
        }
    }

    @Sensitive
    private String makeAcceptResponseHeaderValue() {

        String acceptKey = "";
        try {
            acceptKey = Utils.makeAcceptResponseHeaderValue(headerSecWebSocketKey);
        } catch (NoSuchAlgorithmException e) {
            // allow instrumented FFDC to be used here
        } catch (UnsupportedEncodingException e) {
            // allow instrumented FFDC to be used here
        }

        return acceptKey;
    }

    public static List<Extension> parseClientExtensions(List<String> list) {

        List<Extension> extensions = new ArrayList<Extension>(10);
        String headerSecWebSocketExtensions = null;
        for (String val : list) {
            if (headerSecWebSocketExtensions == null) {
                headerSecWebSocketExtensions = val;
            }
            else {
                headerSecWebSocketExtensions = headerSecWebSocketExtensions + subProtocolDelimiter + val;
            }
        }

        if ((headerSecWebSocketExtensions != null) && (headerSecWebSocketExtensions.length() > 0)) {
            String[] extArr = headerSecWebSocketExtensions.split(subProtocolDelimiter);

            for (int i = 0; i < extArr.length; i++) {
                String[] parameters = extArr[i].trim().split(";");
                String extName = parameters[0];
                List<Extension.Parameter> paramList = new ArrayList<Extension.Parameter>(parameters.length - 1);
                if (parameters.length > 1) {
                    for (int z = 1; z < parameters.length; z++) {
                        String[] namevals = parameters[z].trim().split("=");
                        String val = "";
                        if (namevals.length > 1) {
                            val = namevals[1];
                        }
                        paramList.add(new ParameterExt(namevals[0], val));
                    }
                }
                extensions.add(new ExtensionExt(extName, paramList));

            }
        }
        return extensions;
    }
}
