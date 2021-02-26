/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.frontend.WSDLGetOutInterceptor;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
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

    private Interceptor<Message> wsdlGetOutInterceptor = WSDLGetOutInterceptor.INSTANCE;
    private static final String TRANSFORM_SKIP = "transform.skip";
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


    private Document getDocument(WSDLGetUtils utils,
                                Message message, String base,
                                Map<String, String> params, String ctxUri) {
       // cannot have two wsdl's being generated for the same endpoint at the same
       // time as the addresses may get mixed up
       // For WSDL's the WSDLWriter does not share any state between documents.
       // For XSD's, the WSDLGetUtils makes a copy of any XSD schema documents before updating
       // any addresses and returning them, so for both WSDL and XSD this is the only part that needs
       // to be synchronized.
       synchronized (message.getExchange().getEndpoint()) {
           return utils.getDocument(message, base, params, ctxUri,
                                    message.getExchange().getEndpoint().getEndpointInfo());
       }
   }

    /*
     * Extend from the base class and just customize the response to 404
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String)message.get(Message.QUERY_STRING);

        if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
            return;
        }

        String baseUri = (String)message.get(Message.REQUEST_URL);
        String ctx = (String)message.get(Message.PATH_INFO);

        WSDLGetUtils utils = (WSDLGetUtils)message.getContextualProperty(WSDLGetUtils.class.getName());
        if (utils == null) {
            utils = new WSDLGetUtils();
            message.put(WSDLGetUtils.class, utils);
        }
        Map<String, String> map = UrlUtils.parseQueryString(query);
        if (isRecognizedQuery(map)) {
            Document doc = getDocument(utils, message, baseUri, map, ctx);

            Endpoint e = message.getExchange().getEndpoint();
            Message mout = new MessageImpl();
            mout.setExchange(message.getExchange());
            mout = e.getBinding().createMessage(mout);
            mout.setInterceptorChain(OutgoingChainInterceptor.getOutInterceptorChain(message.getExchange()));
            message.getExchange().setOutMessage(mout);

            mout.put(DOCUMENT_HOLDER, doc);
            mout.put(Message.CONTENT_TYPE, "text/xml");
            
            mout.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_NOT_FOUND);

            // just remove the interceptor which should not be used
            cleanUpOutInterceptors(mout);

            // notice this is being added after the purge above, don't swap the order!
            mout.getInterceptorChain().add(wsdlGetOutInterceptor);

            message.getExchange().put(TRANSFORM_SKIP, Boolean.TRUE);
            // skip the service executor and goto the end of the chain.
            message.getInterceptorChain().doInterceptStartingAt(
                    message,
                    OutgoingChainInterceptor.class.getName());
        }
    }

    // Replaced the isRecognizedQuery() from CXF 2.6.2 with this method which
    // is a direct copy of the WSDLGetInterceptor.isRecognizedQuery(map) in CXF 3.3
    // I was required to copy it as it changed from a public to private method from CXF 2.6 -> CXF 3.3
    private boolean isRecognizedQuery(Map<String, String> map) {
        return map.containsKey("wsdl") || map.containsKey("xsd");
    }

}
