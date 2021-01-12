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

package org.apache.cxf.jaxws;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.w3c.dom.Element;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.ws.addressing.Names;

public class EndpointReferenceBuilder {
    private static final Logger LOG = LogUtils.getL7dLogger(EndpointReferenceBuilder.class);
    private final JaxWsEndpointImpl endpoint;

    public EndpointReferenceBuilder(JaxWsEndpointImpl e) {
        this.endpoint = e;
    }
    public EndpointReference getEndpointReference() {

        //if there is epr in wsdl, direct return this EPR
        List<ExtensibilityElement> portExtensors = endpoint.getEndpointInfo()
            .getExtensors(ExtensibilityElement.class);
        if (portExtensors != null) {
            Iterator<ExtensibilityElement> extensionElements = portExtensors.iterator();
            QName wsaEpr = new QName(Names.WSA_NAMESPACE_NAME, "EndpointReference");
            while (extensionElements.hasNext()) {
                ExtensibilityElement ext = extensionElements.next();
                if (ext instanceof UnknownExtensibilityElement && wsaEpr.equals(ext.getElementType())) {
                    Element eprEle = ((UnknownExtensibilityElement)ext).getElement();
                    List<Element> addressElements = DOMUtils.getChildrenWithName(eprEle,
                                                                                 Names.WSA_NAMESPACE_NAME,
                                                                                 Names.WSA_ADDRESS_NAME);
                    if (!addressElements.isEmpty()) {
                        /*
                         * [WSA-WSDL Binding] : in a SOAP 1.1 port described using WSDL 1.1, the location
                         * attribute of a soap11:address element (if present) would have the same value as the
                         * wsa:Address child element of the wsa:EndpointReference element.
                         */
                        addressElements.get(0).setTextContent(this.endpoint.getEndpointInfo().getAddress());
                    }
                    return EndpointReference.readFrom(new DOMSource(eprEle));
                }

            }
        }


        String bindingId = endpoint.getJaxwsBinding().getBindingID();

        if (!SOAPBindingImpl.isSoapBinding(bindingId)) {
            throw new UnsupportedOperationException(new Message("GET_ENDPOINTREFERENCE_UNSUPPORTED_BINDING",
                                                                LOG, bindingId).toString());
        }

        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        //builder.address(this.endpoint.getEndpointInfo().getAddress()); Liberty change: line removed
        // Liberty change: 2 lines below are added
        String eiAddress = this.endpoint.getEndpointInfo().getAddress();
        builder.address(eiAddress);
        // Liberty change: end


        builder.serviceName(this.endpoint.getService().getName());
        builder.endpointName(this.endpoint.getEndpointInfo().getName());

        if (this.endpoint.getEndpointInfo().getService().getDescription() != null) {
            // builder.wsdlDocumentLocation(this.endpoint.getEndpointInfo().getService().getDescription().getBaseURI()); Liberty change: line removed
            // Liberty change: 2 lines below are added
            String wsdlBaseUri = this.endpoint.getEndpointInfo().getService().getDescription().getBaseURI();
            builder.wsdlDocumentLocation((wsdlBaseUri != null && wsdlBaseUri.startsWith("http://")) ? wsdlBaseUri : eiAddress + "?wsdl");
            // Liberty change: end
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(EndpointReferenceBuilder.class.getClassLoader());

            return builder.build();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        if (clazz != W3CEndpointReference.class) {
            throw new WebServiceException("Unsupported EPR type: " + clazz);
        }
        return clazz.cast(getEndpointReference());
    }
}
