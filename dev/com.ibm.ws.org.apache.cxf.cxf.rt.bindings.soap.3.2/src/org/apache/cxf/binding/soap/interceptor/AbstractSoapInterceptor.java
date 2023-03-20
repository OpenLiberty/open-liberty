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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.Tr;

public abstract class AbstractSoapInterceptor extends AbstractPhaseInterceptor<SoapMessage> implements SoapInterceptor {

    private static final Logger LOG = Logger.getLogger(AbstractSoapInterceptor.class.getName());
    
    private static final TraceComponent tc = Tr.register(AbstractSoapInterceptor.class);

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
        // Liberty Change Start: Reveals only the hidden stack trace. It does not repeat stack trace shown already in the log.
        if (fault.getCause() != null && TraceComponent.isAnyTracingEnabled()) {
            LOG.fine("Fault occured, printing Exception cause to trace.");
            String stackTraceString = buildStackTrace(fault);
            LOG.fine(stackTraceString);
        }// Liberty Change End

        boolean config = MessageUtils.getContextualBoolean(message, Message.FAULT_STACKTRACE_ENABLED, false);
        if (config && fault.getCause() != null) {
            String stackTraceString = buildStackTraceCXF(fault);
            Element detail = fault.getDetail();
            String soapNamespace = message.getVersion().getNamespace();
            if (detail == null) {
                Document doc = DOMUtils.getEmptyDocument();
                Element stackTrace = doc.createElementNS(
                                                         Fault.STACKTRACE_NAMESPACE, Fault.STACKTRACE);
                stackTrace.setTextContent(stackTraceString);
                detail = doc.createElementNS(
                                             soapNamespace, "detail");
                fault.setDetail(detail);
                detail.appendChild(stackTrace);
            } else {
                Element stackTrace = detail.getOwnerDocument().createElementNS(Fault.STACKTRACE_NAMESPACE,
                                                                               Fault.STACKTRACE);
                stackTrace.setTextContent(stackTraceString);
                detail.appendChild(stackTrace);
            }
        }
    }

    /*
     * Liberty Change: code creating standard exception formatted string from stack trace
     * This code reveals only hidden log by CXF
     */
    private String buildStackTrace(SoapFault fault) {
        StringBuilder sb = new StringBuilder();
        Throwable throwable = fault.getCause();
        sb.append(throwable.getClass().getCanonicalName()).append(": ").append(throwable.getMessage()).append("\n");
        while (throwable != null) {
            for (StackTraceElement ste : throwable.getStackTrace()) {
                sb.append(ste.getClassName()).append(".").append(ste.getMethodName()).append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
            }
            throwable = throwable.getCause();
            if (throwable != null) {
                sb.append("Caused by: ").append(throwable.getClass().getCanonicalName()
                         ).append(" : ").append(throwable.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }
    
    /*
     * Liberty Change: Original CXF code creating CXF formatted string from stack trace
     */
    private String buildStackTraceCXF(SoapFault fault) {
        StringBuilder sb = new StringBuilder();
        Throwable throwable = fault.getCause();
        sb.append("Caused by: ").append(throwable.getClass().getCanonicalName()).append(": ").append(throwable.getMessage()).append("\n").append(Message.EXCEPTION_CAUSE_SUFFIX);
        while (throwable != null) {
            for (StackTraceElement ste : throwable.getStackTrace()) {
                sb.append(ste.getClassName()).append("!").append(ste.getMethodName()).append("!").append(ste.getFileName()).append("!"
                         ).append(ste.getLineNumber()).append(Message.EXCEPTION_CAUSE_SUFFIX);
            }
            throwable = throwable.getCause();
            if (throwable != null) {
                sb.append("Caused by: ").append(throwable.getClass().getCanonicalName()
                         ).append(" : ").append(throwable.getMessage()).append(Message.EXCEPTION_CAUSE_SUFFIX);
            }
        }
        return sb.toString();
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
