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
package org.apache.cxf.jaxrs.interceptor;

import java.util.List;
import java.util.ResourceBundle;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.NSStack;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Node;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class JAXRSDefaultFaultOutInterceptor extends AbstractOutDatabindingInterceptor {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSDefaultFaultOutInterceptor.class);

    public JAXRSDefaultFaultOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public JAXRSDefaultFaultOutInterceptor(String phase) {
        super(phase);
    }

    @Override
    @FFDCIgnore(Exception.class)
    public void handleMessage(Message message) throws Fault {
        if (PropertyUtils.isTrue(message.getExchange().get(JAXRSUtils.SECOND_JAXRS_EXCEPTION))) {
            return;
        }
        final Fault f = (Fault) message.getContent(Exception.class);

        Response r = JAXRSUtils.convertFaultToResponse(f.getCause(), message);

        //Liberty Change start - cxf-6373
        if (r == null) //which means it is an unmapped exception
        {
            try {
                AsyncResponseImpl asyncResponse = (AsyncResponseImpl) message.getExchange().getInMessage().get(AsyncResponse.class);
                if (asyncResponse != null)
                {
                    asyncResponse.setUnmappedThrowable(f.getCause());
                }
            } catch (Exception e)
            {//donothing

            }
        }
        //Liberty Change end

        if (r != null) {
            JAXRSUtils.setMessageContentType(message, r);
            message.setContent(List.class, new MessageContentsList(r));
            if (message.getExchange().getOutMessage() == null && message.getExchange().getOutFaultMessage() != null) {
                message.getExchange().setOutMessage(message.getExchange().getOutFaultMessage());
            }
            new JAXRSOutInterceptor().handleMessage(message);
            return;
        }

        ServerProviderFactory.releaseRequestState(message);
        if (mustPropogateException(message)) {
            throw f;
        }

        new StaxOutInterceptor().handleMessage(message);
        message.put(org.apache.cxf.message.Message.RESPONSE_CODE, f.getStatusCode());
        NSStack nsStack = new NSStack();
        nsStack.push();

        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        try {
            nsStack.add("http://cxf.apache.org/bindings/xformat");
            String prefix = nsStack.getPrefix("http://cxf.apache.org/bindings/xformat");
            StaxUtils.writeStartElement(writer, prefix, "XMLFault",
                                        "http://cxf.apache.org/bindings/xformat");
            StaxUtils.writeStartElement(writer, prefix, "faultstring",
                                        "http://cxf.apache.org/bindings/xformat");
            Throwable t = f.getCause();
            writer.writeCharacters(t == null ? f.getMessage() : t.toString());
            // fault string
            writer.writeEndElement();
            // call StaxUtils to write Fault detail.

            if (f.getDetail() != null) {
                StaxUtils.writeStartElement(writer, prefix, "detail", "http://cxf.apache.org/bindings/xformat");
                StaxUtils.writeNode(DOMUtils.getChild(f.getDetail(), Node.ELEMENT_NODE),
                                    writer, false);
                writer.writeEndElement();
            }
            // fault root
            writer.writeEndElement();
            writer.flush();
        } catch (XMLStreamException xe) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), xe);
        }
    }

    @Override
    public void handleFault(Message message) throws Fault {
        if (mustPropogateException(message)) {
            throw (Fault) message.getContent(Exception.class);
        }
    }

    protected boolean mustPropogateException(Message m) {
        return Boolean.TRUE.equals(m.getExchange().get(Message.PROPOGATE_EXCEPTION));
    }

}
