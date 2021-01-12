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

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;

public abstract class AbstractSoapInterceptor extends AbstractPhaseInterceptor<SoapMessage>
    implements SoapInterceptor {

    public AbstractSoapInterceptor(String p) {
        super(p);
    }
    public AbstractSoapInterceptor(String i, String p) {
        super(i, p);
    }


    public Set<URI> getRoles() {
        return Collections.emptySet();
    }

    public Set<QName> getUnderstoodHeaders() {
        return Collections.emptySet();
    }

    protected String getFaultCodePrefix(XMLStreamWriter writer, QName faultCode) throws XMLStreamException {
        String codeNs = faultCode.getNamespaceURI();
        String prefix = null;
        if (codeNs.length() > 0) {
            prefix = faultCode.getPrefix();
            if (!StringUtils.isEmpty(prefix)) {
                String boundNS = writer.getNamespaceContext().getNamespaceURI(prefix);
                if (StringUtils.isEmpty(boundNS)) {
                    writer.writeNamespace(prefix, codeNs);
                } else if (!codeNs.equals(boundNS)) {
                    prefix = null;
                }
            }
            if (StringUtils.isEmpty(prefix)) {
                prefix = StaxUtils.getUniquePrefix(writer, codeNs, true);
            }
        }
        return prefix;
    }

    protected void prepareStackTrace(SoapMessage message, SoapFault fault) throws Exception {
        boolean config = MessageUtils.getContextualBoolean(message, Message.FAULT_STACKTRACE_ENABLED, false);
        if (config && fault.getCause() != null) {
            StringBuilder sb = new StringBuilder();
            Throwable throwable = fault.getCause();
            sb.append("Caused by: ").append(throwable.getClass().getCanonicalName())
                .append(": " + throwable.getMessage() + "\n").append(Message.EXCEPTION_CAUSE_SUFFIX);
            while (throwable != null) {
                for (StackTraceElement ste : throwable.getStackTrace()) {
                    sb.append(ste.getClassName() + "!" + ste.getMethodName() + "!" + ste.getFileName() + "!"
                          + ste.getLineNumber() + "\n");
                }
                throwable = throwable.getCause();
                if (throwable != null) {
                    sb.append("Caused by: " +  throwable.getClass().getCanonicalName()
                              + " : " + throwable.getMessage() + "\n");
                }
            }
            Element detail = fault.getDetail();
            String soapNamespace = message.getVersion().getNamespace();
            if (detail == null) {
                Document doc = DOMUtils.getEmptyDocument();
                Element stackTrace = doc.createElementNS(
                    Fault.STACKTRACE_NAMESPACE, Fault.STACKTRACE);
                stackTrace.setTextContent(sb.toString());
                detail = doc.createElementNS(
                    soapNamespace, "detail");
                fault.setDetail(detail);
                detail.appendChild(stackTrace);
            } else {
                Element stackTrace =
                    detail.getOwnerDocument().createElementNS(Fault.STACKTRACE_NAMESPACE,
                                                              Fault.STACKTRACE);
                stackTrace.setTextContent(sb.toString());
                detail.appendChild(stackTrace);
            }
        }
    }

    static String getFaultMessage(SoapMessage message, SoapFault fault) {
        if (message.get("forced.faultstring") != null) {
            return (String) message.get("forced.faultstring");
        }
        boolean config = MessageUtils.getContextualBoolean(message, Message.EXCEPTION_MESSAGE_CAUSE_ENABLED, false);
        if (fault.getMessage() != null) {
            if (config && fault.getCause() != null
                && fault.getCause().getMessage() != null && !fault.getMessage().equals(fault.getCause().getMessage())) {
                return fault.getMessage() + " Caused by: " + fault.getCause().getMessage();
            }
            return fault.getMessage();
        } else if (config && fault.getCause() != null) {
            if (fault.getCause().getMessage() != null) {
                return fault.getCause().getMessage();
            }
            return fault.getCause().toString();
        } else {
            return "Fault occurred while processing.";
        }
    }
}
