/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.ext.logging.event;

import java.security.AccessController;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.ext.logging.MaskSensitiveHelper;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;

public class DefaultLogEventMapper {
    public static final String MASKED_HEADER_VALUE = "XXX";
    private static final Set<String> DEFAULT_BINARY_CONTENT_MEDIA_TYPES;

    static {
        Set<String> mediaTypes = new HashSet<>(6);
        mediaTypes.add("application/octet-stream");
        mediaTypes.add("application/pdf");
        mediaTypes.add("image/png");
        mediaTypes.add("image/jpeg");
        mediaTypes.add("image/gif");
        mediaTypes.add("image/bmp");
        DEFAULT_BINARY_CONTENT_MEDIA_TYPES = Collections.unmodifiableSet(mediaTypes);
    }
    private static final String MULTIPART_CONTENT_MEDIA_TYPE = "multipart";

    private final Set<String> binaryContentMediaTypes = new HashSet<>(DEFAULT_BINARY_CONTENT_MEDIA_TYPES);

    private MaskSensitiveHelper maskSensitiveHelper = new MaskSensitiveHelper();

    public void addBinaryContentMediaTypes(String mediaTypes) {
        if (mediaTypes != null) {
            Collections.addAll(binaryContentMediaTypes, mediaTypes.split(";"));
        }
    }

    public LogEvent map(final Message message) {
        return this.map(message, Collections.emptySet());
    }

    public LogEvent map(final Message message, final Set<String> sensitiveProtocolHeaders) {
        final LogEvent event = new LogEvent();
        event.setMessageId(getMessageId(message));
        event.setExchangeId((String)message.getExchange().get(LogEvent.KEY_EXCHANGE_ID));
        event.setType(getEventType(message));
        if (!Boolean.TRUE.equals(message.get(Message.DECOUPLED_CHANNEL_MESSAGE))) {
            // avoid logging the default responseCode 200 for the decoupled responses
            Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
            if (responseCode != null) {
                event.setResponseCode(responseCode.toString());
            }
        }

        event.setEncoding(safeGet(message, Message.ENCODING));
        event.setHttpMethod(safeGet(message, Message.HTTP_REQUEST_METHOD));
        event.setContentType(safeGet(message, Message.CONTENT_TYPE));

        Map<String, String> headerMap = getHeaders(message);
        if (sensitiveProtocolHeaders != null && !sensitiveProtocolHeaders.isEmpty()) {
            maskSensitiveHelper.maskHeaders(headerMap, sensitiveProtocolHeaders);
        }
        event.setHeaders(headerMap);

        event.setAddress(getAddress(message, event));

        event.setPrincipal(getPrincipal(message));
        event.setBinaryContent(isBinaryContent(message));
        event.setMultipartContent(isMultipartContent(message));
        setEpInfo(message, event);
        return event;
    }

    private String getPrincipal(Message message) {
        String principal = getJAASPrincipal();
        if (principal != null) {
            return principal;
        }
        SecurityContext sc = message.get(SecurityContext.class);
        if (sc != null && sc.getUserPrincipal() != null) {
            return sc.getUserPrincipal().getName();
        }

        AuthorizationPolicy authPolicy = message.get(AuthorizationPolicy.class);
        if (authPolicy != null) {
            return authPolicy.getUserName();
        }
        return null;
    }

    private String getJAASPrincipal() {
        StringBuilder principals = new StringBuilder();
        Iterator<? extends Object> principalIt = getJAASPrincipals();
        while (principalIt.hasNext()) {
            principals.append(principalIt.next());
            if (principalIt.hasNext()) {
                principals.append(',');
            }
        }
        if (principals.length() == 0) {
            return null;
        }
        return principals.toString();
    }

    private Iterator<? extends Object> getJAASPrincipals() {
        Subject subject = Subject.getSubject(AccessController.getContext());
        return subject != null && subject.getPrincipals() != null
            ? subject.getPrincipals().iterator() : Collections.emptyIterator();
    }

    private Map<String, String> getHeaders(Message message) {
        Map<String, List<Object>> headers = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        Map<String, String> result = new HashMap<>();
        if (headers == null) {
            return result;
        }
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            if (entry.getValue().size() == 1) {
                Object value = entry.getValue().get(0);
                if (value != null) {
                    result.put(entry.getKey(), value.toString());
                }
            } else {
                result.put(entry.getKey(), Arrays.deepToString(entry.getValue().toArray()));
            }
        }
        return result;
    }

    private String getAddress(Message message, LogEvent event) {
        final Message observedMessage;
        if (event.getType() == EventType.RESP_IN) {
            observedMessage = message.getExchange().getOutMessage();
        } else if (event.getType() == EventType.RESP_OUT) {
            observedMessage = message.getExchange().getInMessage();
        } else {
            observedMessage = message;
        }

        return getUri(observedMessage);
    }

    private String getUri(Message message) {
        String uri = safeGet(message, Message.REQUEST_URL);
        if (uri == null) {
            String address = safeGet(message, Message.ENDPOINT_ADDRESS);
            uri = safeGet(message, Message.REQUEST_URI);
            if (uri != null && uri.startsWith("/")) {
                if (address != null && !address.startsWith(uri)) {
                    if (address.endsWith("/") && address.length() > 1) {
                        address = address.substring(0, address.length() - 1);
                    }
                    uri = address + uri;
                }
            } else if (address != null) {
                uri = address;
            }
        }
        String query = safeGet(message, Message.QUERY_STRING);
        if (query != null) {
            return uri + "?" + query;
        }
        return uri;
    }

    private boolean isBinaryContent(Message message) {
        String contentType = safeGet(message, Message.CONTENT_TYPE);
        return contentType != null && binaryContentMediaTypes.contains(contentType);
    }

    public boolean isBinaryContent(String contentType) {
        return contentType != null && binaryContentMediaTypes.contains(contentType);
    }
    
    private boolean isMultipartContent(Message message) {
        String contentType = safeGet(message, Message.CONTENT_TYPE);
        return contentType != null && contentType.startsWith(MULTIPART_CONTENT_MEDIA_TYPE);
    }

    /**
     * check if a Message is a Rest Message
     *
     * @param message
     * @return
     */
    private boolean isSOAPMessage(Message message) {
        Binding binding = message.getExchange().getBinding();
        return binding != null && "SoapBinding".equals(binding.getClass().getSimpleName());
    }

    /**
     * Get MessageId from WS Addressing properties
     *
     * @param message
     * @return message id
     */
    private String getMessageId(Message message) {
        AddressingProperties addrProp = ContextUtils.retrieveMAPs(message, false,
                                                                  MessageUtils.isOutbound(message), false);
        return addrProp != null && addrProp.getMessageID() != null
            ? addrProp.getMessageID().getValue() : UUID.randomUUID().toString();
    }

    private String getOperationName(Message message) {
        String operationName = null;
        BindingOperationInfo boi = message.getExchange().getBindingOperationInfo();

        if (null != boi) {
            operationName = boi.getName().toString();
        }

        return operationName;
    }

    private Message getEffectiveMessage(Message message) {
        boolean isRequestor = MessageUtils.isRequestor(message);
        boolean isOutbound = MessageUtils.isOutbound(message);
        if (isRequestor) {
            return isOutbound ? message : message.getExchange().getOutMessage();
        }
        return isOutbound ? message.getExchange().getInMessage() : message;
    }

    private String getRestOperationName(Message curMessage) {
        Message message = getEffectiveMessage(curMessage);
        String httpMethod = safeGet(message, Message.HTTP_REQUEST_METHOD);
        if (httpMethod == null) {
            return "";
        }

        String path = "";
        String requestUri = safeGet(message, Message.REQUEST_URI);
        if (requestUri != null) {
            String basePath = safeGet(message, Message.BASE_PATH);
            if (basePath == null) {
                path = requestUri;
            } else if (requestUri.startsWith(basePath)) {
                path = requestUri.substring(basePath.length());
            }
            if (path.isEmpty()) {
                path = "/";
            }
        }
        return new StringBuffer().append(httpMethod).append('[').append(path).append(']').toString();
    }

    private static String safeGet(Message message, String key) {
        if (message == null || !message.containsKey(key)) {
            return null;
        }
        Object value = message.get(key);
        return (value instanceof String) ? value.toString() : null;
    }

    /**
     * Gets the event type from message.
     *
     * @param message the message
     * @return the event type
     */
    public EventType getEventType(Message message) {
        boolean isRequestor = MessageUtils.isRequestor(message);
        boolean isFault = MessageUtils.isFault(message);
        if (!isFault) {
            isFault = !isSOAPMessage(message) && isRESTFault(message);
        }
        boolean isOutbound = MessageUtils.isOutbound(message);
        if (isOutbound) {
            if (isFault) {
                return EventType.FAULT_OUT;
            }
            return isRequestor ? EventType.REQ_OUT : EventType.RESP_OUT;
        }
        if (isFault) {
            return EventType.FAULT_IN;
        }
        return isRequestor ? EventType.RESP_IN : EventType.REQ_IN;
    }

    /**
     * For REST we also consider a response to be a fault if the operation is not found or the response code
     * is an error
     *
     * @param message
     * @return
     */
    private boolean isRESTFault(Message message) {
        Object opName = message.getExchange().get("org.apache.cxf.resource.operation.name");
        Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
        if (opName == null && responseCode == null) {
            return true;
        }
        return (responseCode != null) && (responseCode >= 400);
    }

    public void setEpInfo(Message message, final LogEvent event) {
        EndpointInfo endpoint = getEPInfo(message);
        event.setPortName(endpoint.getName());
        event.setPortTypeName(endpoint.getName());
        String opName = isSOAPMessage(message) ? getOperationName(message) : getRestOperationName(message);
        event.setOperationName(opName);
        if (endpoint.getService() != null) {
            setServiceInfo(endpoint.getService(), event);
        }
    }

    private void setServiceInfo(ServiceInfo service, LogEvent event) {
        event.setServiceName(service.getName());
        InterfaceInfo iface = service.getInterface();
        event.setPortTypeName(iface.getName());
    }

    private EndpointInfo getEPInfo(Message message) {
        Endpoint ep = message.getExchange().getEndpoint();
        return (ep == null) ? new EndpointInfo() : ep.getEndpointInfo();
    }

}
