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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;

import static org.apache.cxf.message.Message.MIME_HEADERS;

/**
 * This interceptor is responsible for setting up the SOAP version
 * and header, so that this is available to any pre-protocol interceptors
 * that require these to be available.
 */
public class SoapPreProtocolOutInterceptor extends AbstractSoapInterceptor {

    public SoapPreProtocolOutInterceptor() {
        super(Phase.POST_LOGICAL);
    }

    /**
     * Mediate a message dispatch.
     *
     * @param message the current message
     * @throws Fault
     */
    public void handleMessage(SoapMessage message) throws Fault {
        ensureVersion(message);
        ensureMimeHeaders(message);
        if (isRequestor(message)) {
            setSoapAction(message);
        }

    }

    /**
     * Ensure the SOAP version is set for this message.
     *
     * @param message the current message
     */
    private void ensureVersion(SoapMessage message) {
        SoapVersion soapVersion = message.getVersion();
        if (soapVersion == null
            && message.getExchange().getInMessage() instanceof SoapMessage) {
            soapVersion = ((SoapMessage)message.getExchange().getInMessage()).getVersion();
            message.setVersion(soapVersion);
        }

        if (soapVersion == null) {
            soapVersion = Soap11.getInstance();
            message.setVersion(soapVersion);
        }

        message.put(Message.CONTENT_TYPE, (String) soapVersion.getContentType());
    }

    /**
     * Ensure the SOAP header is set for this message.
     *
     * @param message the current message
     */
    private void ensureMimeHeaders(SoapMessage message) {
        if (message.get(MIME_HEADERS) == null) {
            message.put(MIME_HEADERS, new HashMap<String, List<String>>());
        }
        String cte = (String)message.getContextualProperty(Message.CONTENT_TRANSFER_ENCODING);
        if (cte != null) {
            //root part MUST be binary
            message.put(Message.CONTENT_TRANSFER_ENCODING, "binary");
            message.put("soap.attachement.content.transfer.encoding", cte);
        }
    }

    private void setSoapAction(SoapMessage message) {
        BindingOperationInfo boi = message.getExchange().getBindingOperationInfo();

        // The soap action is set on the wrapped operation.
        if (boi != null && boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }

        String action = getSoapAction(message, boi);

        if (message.getVersion() instanceof Soap11) {
            Map<String, List<String>> tempReqHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, List<String>> reqHeaders
                    = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            if (reqHeaders != null) {
                tempReqHeaders.putAll(reqHeaders);
            }
            if (!tempReqHeaders.containsKey(SoapBindingConstants.SOAP_ACTION)) {
                tempReqHeaders.put(SoapBindingConstants.SOAP_ACTION, Collections.singletonList(action));
            }
            message.put(Message.PROTOCOL_HEADERS, tempReqHeaders);
        } else if (message.getVersion() instanceof Soap12 && !"\"\"".equals(action)) {
            String ct = (String) message.get(Message.CONTENT_TYPE);

            if (ct.indexOf("action=\"") == -1) {
                ct = new StringBuilder().append(ct)
                    .append("; action=").append(action).toString();
                message.put(Message.CONTENT_TYPE, ct);
            }
        }
    }

    private String getSoapAction(SoapMessage message, BindingOperationInfo boi) {
        // allow an interceptor to override the SOAPAction if need be
        String action = (String) message.get(SoapBindingConstants.SOAP_ACTION);

        // Fall back on the SOAPAction in the operation info
        if (action == null) {
            if (boi == null) {
                action = "\"\"";
            } else {
                SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
                action = soi == null ? "\"\"" : soi.getAction() == null ? "\"\"" : soi.getAction();
            }
        }

        if (!action.startsWith("\"")) {
            action = new StringBuilder().append("\"").append(action).append("\"").toString();
        }

        return action;
    }

}
