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
package com.ibm.ws.jaxws.globalhandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Service;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJFactoryResolver;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.logical.LogicalMessageContextImpl;
import org.apache.cxf.jaxws.handler.logical.LogicalMessageImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.XMLMessage;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.wsdl.util.xml.DOMUtils;

/**
 *
 */
public class GlobalHandlerLogicalMessageImpl extends LogicalMessageImpl {
    private final LogicalMessageContextImpl msgContext;
    private static final Logger LOG = LogUtils.getL7dLogger(GlobalHandlerLogicalMessageImpl.class);

    /**
     * @param lmctx
     */
    public GlobalHandlerLogicalMessageImpl(LogicalMessageContextImpl lmctx) {
        super(lmctx);
        msgContext = lmctx;
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setPayload(Source s) {
        Message message = msgContext.getWrappedMessage();
        Service.Mode mode = (Service.Mode) msgContext.getWrappedMessage()
                        .getContextualProperty(Service.Mode.class.getName());
        SOAPMessage m = message.getContent(SOAPMessage.class);
        if (m != null) {
            try {
                SAAJUtils.getBody(m).removeContents();
                W3CDOMStreamWriter writer = new W3CDOMStreamWriter(SAAJUtils.getBody(m));
                StaxUtils.copy(s, writer);
                writer.flush();
                writer.close();
                if (mode == Service.Mode.MESSAGE) {
                    s = new DOMSource(m.getSOAPPart());
                } else {
                    s = new DOMSource(SAAJUtils.getBody(m).getFirstChild());
                }
                W3CDOMStreamReader r = new W3CDOMStreamReader(DOMUtils.getFirstChildElement(SAAJUtils.getBody(m)));
                message.setContent(XMLStreamReader.class, r);
            } catch (Exception e) {
                throw new Fault(e);
            }
        } else if (mode != null) {
            if (message instanceof SoapMessage) {
                if (mode == Service.Mode.MESSAGE) {
                    try {
                        // REVISIT: should try to use the original SOAPMessage
                        // instead of creating a new empty one.
                        SOAPMessage msg = initSOAPMessage(null);
                        write(s, SAAJUtils.getBody(msg));
                        s = new DOMSource(msg.getSOAPPart());
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                }
            } else if (message instanceof XMLMessage && message.getContent(DataSource.class) != null) {
                throw new Fault(
                                new org.apache.cxf.common.i18n.Message(
                                                "GETPAYLOAD_OF_DATASOURCE_NOT_VALID_XMLHTTPBINDING",
                                                LOG));
            }
        } else {
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(s);
            msgContext.getWrappedMessage().setContent(XMLStreamReader.class, reader);
        }
        msgContext.getWrappedMessage().setContent(Source.class, s);
    }

    private SOAPMessage initSOAPMessage(InputStream is) throws SOAPException, IOException {
        SOAPMessage msg = null;
        if (is != null) {
            msg = SAAJFactoryResolver.createMessageFactory(null).createMessage(null, is);
        } else {
            msg = SAAJFactoryResolver.createMessageFactory(null).createMessage();
        }
        msg.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        msg.getSOAPPart().getEnvelope().addNamespaceDeclaration(WSDLConstants.NP_SCHEMA_XSD,
                                                                WSDLConstants.NS_SCHEMA_XSD);
        msg.getSOAPPart().getEnvelope().addNamespaceDeclaration(WSDLConstants.NP_SCHEMA_XSI,
                                                                WSDLConstants.NS_SCHEMA_XSI);

        return msg;
    }

    private void write(Source source, Node n) {
        try {
            if (source instanceof DOMSource && ((DOMSource) source).getNode() == null) {
                return;
            }

            XMLStreamWriter writer = new W3CDOMStreamWriter((Element) n);
            StaxUtils.copy(source, writer);
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }

}
