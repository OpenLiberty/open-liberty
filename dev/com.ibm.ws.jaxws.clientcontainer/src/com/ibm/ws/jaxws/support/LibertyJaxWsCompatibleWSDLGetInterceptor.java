/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.Conduit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * JaxWsComptibileWSDLGetInterceptor provides a JaxWs compatible implementation for WSDL query, the difference is:
 * a. Report an error if the binding is not of SOAP 1.1 binding type, and the WSDL file is not included in the user applications.
 * b. Report an error If wsdlLocation is configured for the target endpoint, while the physical file could not be located.
 */

public class LibertyJaxWsCompatibleWSDLGetInterceptor extends WSDLGetInterceptor {

    private static final TraceComponent tc = Tr.register(LibertyJaxWsCompatibleWSDLGetInterceptor.class);

    private static final Document noWSDLDoc;

    private final String implBeanClazzName;

    private final String wsdlLocation;

    private final boolean wsdlLoationExisted;

    private final Document noWSDLLocationDoc;

    static {
        noWSDLDoc = DOMUtils.createDocument();
        Element root = noWSDLDoc.createElement("Error");
        root.setTextContent(Tr.formatMessage(tc, "error.no.wsdl.per.specification"));
        noWSDLDoc.appendChild(root);
    }

    /**
     * @param implBeanClazz
     * @param isWsdlChanged
     */
    public LibertyJaxWsCompatibleWSDLGetInterceptor(String implBeanClazzName, String wsdlLocation, boolean wsdlLoationExisted) {
        this.implBeanClazzName = implBeanClazzName;
        this.wsdlLocation = wsdlLocation;
        this.wsdlLoationExisted = wsdlLoationExisted;
        noWSDLLocationDoc = DOMUtils.createDocument();
        if (!wsdlLoationExisted) {
            Element root = noWSDLLocationDoc.createElement("Error");
            root.setTextContent(Tr.formatMessage(tc, "error.no.wsdl.find", new Object[] { wsdlLocation, implBeanClazzName }));
            noWSDLLocationDoc.appendChild(root);
        }

    }

    @Override
    public Document getDocument(Message message, String base, Map<String, String> params, String ctxUri, EndpointInfo endpointInfo) {

        if (wsdlLoationExisted) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "warn.no.wsdl.generate", implBeanClazzName);
            }
            return noWSDLDoc;
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "error.no.wsdl.find", new Object[] { wsdlLocation, implBeanClazzName });
            }
            return noWSDLLocationDoc;
        }
    }

    /*
     * Extend from the base class and just customize the response to 404
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String) message.get(Message.QUERY_STRING);
        if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
            return;
        }
        String baseUri = (String) message.get(Message.REQUEST_URL);
        String ctx = (String) message.get(Message.PATH_INFO);

        //cannot have two wsdl's being written for the same endpoint at the same
        //time as the addresses may get mixed up
        synchronized (message.getExchange().getEndpoint()) {
            Map<String, String> map = UrlUtils.parseQueryString(query);
            if (isRecognizedQuery(map, baseUri, ctx,
                                  message.getExchange().getEndpoint().getEndpointInfo())) {

                try {
                    Conduit c = message.getExchange().getDestination().getBackChannel(message, null, null);
                    Message mout = new MessageImpl();
                    mout.setExchange(message.getExchange());
                    message.getExchange().setOutMessage(mout);
                    //Customize the response to 404
                    mout.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_NOT_FOUND);
                    mout.put(Message.CONTENT_TYPE, "text/xml");
                    c.prepare(mout);
                    OutputStream os = mout.getContent(OutputStream.class);

                    Document doc = getDocument(message,
                                               baseUri,
                                               map,
                                               ctx,
                                               message.getExchange().getEndpoint().getEndpointInfo());

                    String enc = null;
                    try {
                        enc = doc.getXmlEncoding();
                    } catch (Exception ex) {
                        //ignore - not dom level 3
                    }
                    if (enc == null) {
                        enc = "utf-8";
                    }

                    XMLStreamWriter writer = null;
                    try {
                        writer = StaxUtils.createXMLStreamWriter(os, enc);
                        StaxUtils.writeNode(doc, writer, true);
                        message.getInterceptorChain().abort();
                        writer.flush();
                        os.flush();
                    } catch (IOException ex) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                            Tr.info(tc, "error.write.wsdl.stream", ex);
                        }
                        //LOG.log(Level.FINE, "Failure writing full wsdl to the stream", ex);
                        //we can ignore this.   Likely, whatever has requested the WSDL
                        //has closed the connection before reading the entire wsdl.  
                        //WSDL4J has a tendency to not read the closing tags and such
                        //and thus can sometimes hit this.   In anycase, it's 
                        //pretty much ignorable and nothing we can do about it (cannot
                        //send a fault or anything anyway
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (Exception e) {
                            }
                        }
                        try {
                            os.close();
                        } catch (Exception e) {
                        }
                    }
                } catch (IOException e) {
                    throw new Fault(e);
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                } finally {
                    message.getExchange().setOutMessage(null);
                }
            }
        }
    }

}
