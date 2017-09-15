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
package com.ibm.ws.wsoc.outbound;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.HandshakeProcessor;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.WebSocketContainerManager;
import com.ibm.ws.wsoc.external.HandshakeResponseExt;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 *
 */
public class HttpRequestor {

    private static final TraceComponent tc = Tr.register(HttpRequestor.class);

    private ClientTransportAccess access = null;

    private WsocAddress endpointAddress = null;

    private OutboundVirtualConnection vc = null;

    private HttpOutboundServiceContext httpOutboundSC = null;

    private String websocketKey = "";

    private final Map<String, List<String>> requestHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

    private Map<String, List<String>> responseHeaders = null;

    private final ClientEndpointConfig config;

    private final ParametersOfInterest things;

    public HttpRequestor(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        this.endpointAddress = endpointAddress;
        this.config = config;
        this.things = things;
    }

    public ClientTransportAccess getClientTransportAccess() {
        return access;
    }

    public void connect() throws Exception {

        access = new ClientTransportAccess();

        vc = (OutboundVirtualConnection) WsocOutboundChain.getVCFactory(endpointAddress);;
        access.setVirtualConnection(vc);
        vc.connect(endpointAddress);

    }

    public void sendRequest() throws IOException, MessageSentException {

        httpOutboundSC = (HttpOutboundServiceContext) vc.getChannelAccessor();
        //httpOutboundSC = (HttpOutboundServiceContextImpl) vc.getChannelAccessor();
        access.setTCPConnectionContext(httpOutboundSC.getTSC());
        access.setDeviceConnLink(httpOutboundSC.getLink());

        HttpRequestMessage hrm = httpOutboundSC.getRequest();
        hrm.setRequestURI(endpointAddress.getPath());
        hrm.setVersion(VersionValues.V11);
        hrm.setMethod(MethodValues.GET);

        //   We put request headers in Map for possible modification by configurator beforeRequest, and also used by Session
        requestHeaders.put(HttpHeaderKeys.HDR_CONNECTION.getName(), Arrays.asList(Constants.HEADER_VALUE_UPGRADE));
        requestHeaders.put(HttpHeaderKeys.HDR_UPGRADE.getName(), Arrays.asList(Constants.HEADER_VALUE_WEBSOCKET));
        requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION, Arrays.asList(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION));

        websocketKey = WebSocketContainerManager.getRef().generateWebsocketKey();
        requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_KEY, Arrays.asList(websocketKey));

        if (config != null) {
            List<String> subprotocols = config.getPreferredSubprotocols();
            if (subprotocols != null) {
                if (subprotocols.size() > 0) {
                    String subprotocolValue = "";

                    for (int x = 0; x < subprotocols.size(); x++) {
                        if (x == 0) {
                            subprotocolValue = subprotocols.get(0).trim();
                        } else {
                            subprotocolValue = subprotocolValue + "," + subprotocols.get(x).trim();
                        }
                    }
                    requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_PROTOCOL, Arrays.asList(subprotocolValue));
                }
            }

            List<Extension> extensions = config.getExtensions();

            if (extensions != null) {
                if (extensions.size() > 0) {
                    StringBuffer buf = new StringBuffer();
                    boolean first = true;
                    for (Extension ext : extensions) {
                        if (first) {
                            first = false;
                        } else {
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
                    requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS, Arrays.asList(buf.toString()));
                }
            }

            if (config.getConfigurator() != null) {
                config.getConfigurator().beforeRequest(requestHeaders);
            }
        }
        Iterator<Entry<String, List<String>>> i = requestHeaders.entrySet().iterator();
        while (i.hasNext()) {
            Entry<String, List<String>> entry = i.next();
            List<String> list = entry.getValue();

            if (list == null) {
                hrm.setHeader(entry.getKey(), "");
                continue;
            }
            if (list.size() == 0) {
                hrm.setHeader(entry.getKey(), "");
            }
            for (int x = 0; x < list.size(); x++) {
                if (x == 0) {
                    hrm.setHeader(entry.getKey(), list.get(x));
                } else {
                    hrm.appendHeader(entry.getKey(), list.get(x));
                }
            }
        }

        httpOutboundSC.enableImmediateResponseRead();
        httpOutboundSC.sendRequestHeaders();

    }

    public WsByteBuffer completeResponse() throws IOException {
        HttpResponseMessage hrm = httpOutboundSC.getResponse();

        if (!StatusCodes.SWITCHING_PROTOCOLS.equals(hrm.getStatusCode())) {
            String msg = Tr.formatMessage(tc, "client.invalid.returncode", hrm.getStatusCode(),
                                          endpointAddress.getURI().toString());
            Tr.error(tc, "client.invalid.returncode", hrm.getStatusCode(),
                     endpointAddress.getURI().toString());
            throw new IOException(msg);
        }

        String acceptKey;
        try {
            acceptKey = Utils.makeAcceptResponseHeaderValue(websocketKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        String key = hrm.getHeader(Constants.MC_HEADER_NAME_SEC_WEBSOCKET_ACCEPT).asString();
        if (key != null) {
            if (!key.equals(acceptKey)) {
                String msg = Tr.formatMessage(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                                              endpointAddress.getURI().toString());
                Tr.error(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                         endpointAddress.getURI().toString());
                throw new IOException(msg);
            }
        } else {
            String msg = Tr.formatMessage(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                                          endpointAddress.getURI().toString());
            Tr.error(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                     endpointAddress.getURI().toString());
            throw new IOException(msg);
        }

        if (config != null) {
            Collection<String> names = hrm.getAllHeaderNames();
            responseHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            Iterator<String> it = names.iterator();
            while (it.hasNext()) {
                String name = it.next();

                List<HeaderField> hdrs = hrm.getHeaders(name);
                List<String> values = new ArrayList<String>(hdrs.size());
                for (HeaderField header : hdrs) {
                    values.add(header.asString());
                }

                // Check for sub protocol here so case will match
                if (name.equalsIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_PROTOCOL)) {
                    if (values != null) {
                        if (values.size() >= 1) {
                            things.setAgreedSubProtocol(values.get(0));
                        }
                    }
                }

                // Check for extensions here so case will match
                if (name.equalsIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS)) {
                    if (values != null) {
                        if (values.size() >= 1) {
                            things.setNegotiatedExtensions(HandshakeProcessor.parseClientExtensions(values));
                        }
                    }
                }

                responseHeaders.put(name, values);
            }

            if (config.getConfigurator() != null) {
                HandshakeResponseExt handshakeResponse = new HandshakeResponseExt(responseHeaders);
                config.getConfigurator().afterResponse(handshakeResponse);
            }
        }

        WsByteBuffer retBuf = httpOutboundSC.getRemainingData();

        // Finish adding needed info, this will be used during Session calls - IE session.getParameterMap, etc
        things.setURI(endpointAddress.getURI());
        things.setParameterMap(null);
        if (config != null) {
            things.setLocalSubProtocols(config.getPreferredSubprotocols());
        }
        things.setWsocProtocolVersion(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION);
        things.setSecure(endpointAddress.isSecure());

        return retBuf;

    }

    void closeConnection(IOException ioe) {
        if (access != null) {
            if (access.getDeviceConnLink() != null) {
                access.getDeviceConnLink().close(vc, ioe);
            }
        }
    }

}
