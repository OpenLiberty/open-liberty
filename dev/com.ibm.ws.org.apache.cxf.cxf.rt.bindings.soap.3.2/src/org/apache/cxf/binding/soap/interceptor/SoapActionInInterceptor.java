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

package org.apache.cxf.binding.soap.interceptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSInInterceptor;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.ws.addressing.JAXWSAConstants;

public class SoapActionInInterceptor extends AbstractSoapInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(SoapActionInInterceptor.class);
    private static final String ALLOW_NON_MATCHING_TO_DEFAULT = "allowNonMatchingToDefaultSoapAction";
    private static final String CALCULATED_WSA_ACTION = SoapActionInInterceptor.class.getName() + ".ACTION";

    public SoapActionInInterceptor() {
        super(Phase.READ);
        addAfter(ReadHeadersInterceptor.class.getName());
        addAfter(EndpointSelectionInterceptor.class.getName());
    }

    public static String getSoapAction(Message m) {
        if (!(m instanceof SoapMessage)) {
            return null;
        }
        SoapMessage message = (SoapMessage)m;
        if (message.getVersion() instanceof Soap11) {
            Map<String, List<String>> headers
                = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            if (headers != null) {
                List<String> sa = headers.get(SoapBindingConstants.SOAP_ACTION);
                if (sa != null && !sa.isEmpty()) {
                    String action = sa.get(0);
                    if (action.startsWith("\"") || action.startsWith("\'")) {
                        action = action.substring(1, action.length() - 1);
                    }
                    return action;
                }
            }
        } else if (message.getVersion() instanceof Soap12) {
            String ct = (String) message.get(Message.CONTENT_TYPE);

            if (ct == null) {
                return null;
            }

            int start = ct.indexOf("action=");
            if (start == -1 && ct.indexOf("multipart/related") == 0 && ct.indexOf("start-info") == -1) {
                // the action property may not be found at the package's content-type for non-mtom multipart message
                // but skip searching if the start-info property is set
                List<String> cts = CastUtils.cast((List<?>)(((Map<?, ?>)
                    message.get(AttachmentDeserializer.ATTACHMENT_PART_HEADERS)).get(Message.CONTENT_TYPE)));
                if (cts != null && !cts.isEmpty()) {
                    ct = cts.get(0);
                    start = ct.indexOf("action=");
                }
            }
            if (start != -1) {
                int end;
                char c = ct.charAt(start + 7);
                // handle the extraction robustly
                if (c == '\"') {
                    start += 8;
                    end = ct.indexOf('\"', start);
                } else if (c == '\\' && ct.charAt(start + 8) == '\"') {
                    start += 9;
                    end = ct.indexOf('\\', start);
                } else {
                    start += 7;
                    end = ct.indexOf(';', start);
                    if (end == -1) {
                        end = ct.length();
                    }
                }
                return ct.substring(start, end);
            }
        }

        // Return the Soap Action for the JMS Case
        if (message.containsKey(SoapJMSInInterceptor.JMS_SOAP_ACTION_VALUE)) {
            return (String)message.get(SoapJMSInInterceptor.JMS_SOAP_ACTION_VALUE);
        }

        return null;
    }

    public void handleMessage(SoapMessage message) throws Fault {
        if (isRequestor(message)) {
            return;
        }

        String action = getSoapAction(message);
        if (!StringUtils.isEmpty(action)) {
            getAndSetOperation(message, action);
            message.put(SoapBindingConstants.SOAP_ACTION, action);
        }
    }

    public static void getAndSetOperation(SoapMessage message, String action) {
        getAndSetOperation(message, action, true);
    }
    public static void getAndSetOperation(SoapMessage message, String action, boolean strict) {
        if (StringUtils.isEmpty(action)) {
            return;
        }

        Exchange ex = message.getExchange();
        Endpoint ep = ex.getEndpoint();
        if (ep == null) {
            return;
        }

        BindingOperationInfo bindingOp = null;

        Collection<BindingOperationInfo> bops = ep.getEndpointInfo()
            .getBinding().getOperations();
        if (bops != null) {
            for (BindingOperationInfo boi : bops) {
                if (isActionMatch(message, boi, action)) {
                    if (bindingOp != null) {
                        //more than one op with the same action, will need to parse normally
                        return;
                    }
                    bindingOp = boi;
                }
                if (matchWSAAction(boi, action)) {
                    if (bindingOp != null && bindingOp != boi) {
                        //more than one op with the same action, will need to parse normally
                        return;
                    }
                    bindingOp = boi;
                }
            }
        }

        if (bindingOp == null) {
            if (strict) {
                //we didn't match the an operation, we'll try again later to make
                //sure the incoming message did end up matching an operation.
                //This could occur in some cases like WS-RM and WS-SecConv that will
                //intercept the message with a new endpoint/operation
                message.getInterceptorChain().add(new SoapActionInAttemptTwoInterceptor(action));
            }
            return;
        }

        ex.put(BindingOperationInfo.class, bindingOp);
    }
    private static boolean matchWSAAction(BindingOperationInfo boi, String action) {
        Object o = getWSAAction(boi);
        if (o != null) {
            String oa = o.toString();
            if (action.equals(oa)
                || action.equals(oa + "Request")
                || oa.equals(action + "Request")) {
                return true;
            }
        }
        return false;
    }

    private static String getWSAAction(BindingOperationInfo boi) {
        Object o = boi.getOperationInfo().getInput().getProperty(CALCULATED_WSA_ACTION);
        if (o == null) {
            o = boi.getOperationInfo().getInput().getExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME);
            if (o == null) {
                o = boi.getOperationInfo().getInput().getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME);
            }
            if (o == null) {
                String start = getActionBaseUri(boi.getOperationInfo());
                if (null == boi.getOperationInfo().getInputName()) {
                    o = addPath(start, boi.getOperationInfo().getName().getLocalPart());
                } else {
                    o = addPath(start, boi.getOperationInfo().getInputName());
                }
            }
            if (o != null) {
                boi.getOperationInfo().getInput().setProperty(CALCULATED_WSA_ACTION, o);
            }
        }
        return o.toString();
    }
    private static String getActionBaseUri(final OperationInfo operation) {
        String interfaceName = operation.getInterface().getName().getLocalPart();
        return addPath(operation.getName().getNamespaceURI(), interfaceName);
    }
    private static String getDelimiter(String uri) {
        if (uri.startsWith("urn")) {
            return ":";
        }
        return "/";
    }

    private static String addPath(String uri, String path) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(uri);
        String delimiter = getDelimiter(uri);
        if (!uri.endsWith(delimiter) && !path.startsWith(delimiter)) {
            buffer.append(delimiter);
        }
        buffer.append(path);
        return buffer.toString();
    }


    public static class SoapActionInAttemptTwoInterceptor extends AbstractSoapInterceptor {
        final String action;
        public SoapActionInAttemptTwoInterceptor(String action) {
            super(action, Phase.PRE_LOGICAL);
            this.action = action;
        }
        public void handleMessage(SoapMessage message) throws Fault {
            BindingOperationInfo boi = message.getExchange().getBindingOperationInfo();
            if (boi == null) {
                return;
            }
            if (StringUtils.isEmpty(action)) {
                return;
            }
            if (isActionMatch(message, boi, action)) {
                return;
            }
            if (matchWSAAction(boi, action)) {
                return;
            }
            boolean synthetic = Boolean.TRUE.equals(boi.getProperty("operation.is.synthetic"));
            if (!synthetic) {
                throw new Fault("SOAP_ACTION_MISMATCH", LOG, null, action);
            }
        }
    }

    private static boolean isActionMatch(SoapMessage message, BindingOperationInfo boi, String action) {
        SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
        if (soi == null) {
            return false;
        }
        boolean allowNoMatchingToDefault = MessageUtils.getContextualBoolean(message,
                                                                    ALLOW_NON_MATCHING_TO_DEFAULT,
                                                                    false);
        return action.equals(soi.getAction())
               || (allowNoMatchingToDefault && StringUtils.isEmpty(soi.getAction())
               || (message.getVersion() instanceof Soap12) && StringUtils.isEmpty(soi.getAction()));
    }

}