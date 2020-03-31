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
package com.ibm.ws.wsat.threadedclient.client.threaded;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsat.threadedclient.client.threaded package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Invoke_QNAME = new QName("http://server.threadedserver.wsat.ws.ibm.com/", "invoke");
    private final static QName _InvokeResponse_QNAME = new QName("http://server.threadedserver.wsat.ws.ibm.com/", "invokeResponse");
    private final static QName _ClearXAResourceResponse_QNAME = new QName("http://server.threadedserver.wsat.ws.ibm.com/", "clearXAResourceResponse");
    private final static QName _ClearXAResource_QNAME = new QName("http://server.threadedserver.wsat.ws.ibm.com/", "clearXAResource");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsat.threadedclient.client.threaded
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link InvokeResponse }
     * 
     */
    public InvokeResponse createInvokeResponse() {
        return new InvokeResponse();
    }

    /**
     * Create an instance of {@link Invoke }
     * 
     */
    public Invoke createInvoke() {
        return new Invoke();
    }

    /**
     * Create an instance of {@link ClearXAResource }
     * 
     */
    public ClearXAResource createClearXAResource() {
        return new ClearXAResource();
    }

    /**
     * Create an instance of {@link ClearXAResourceResponse }
     * 
     */
    public ClearXAResourceResponse createClearXAResourceResponse() {
        return new ClearXAResourceResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Invoke }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.threadedserver.wsat.ws.ibm.com/", name = "invoke")
    public JAXBElement<Invoke> createInvoke(Invoke value) {
        return new JAXBElement<Invoke>(_Invoke_QNAME, Invoke.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.threadedserver.wsat.ws.ibm.com/", name = "invokeResponse")
    public JAXBElement<InvokeResponse> createInvokeResponse(InvokeResponse value) {
        return new JAXBElement<InvokeResponse>(_InvokeResponse_QNAME, InvokeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ClearXAResourceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.threadedserver.wsat.ws.ibm.com/", name = "clearXAResourceResponse")
    public JAXBElement<ClearXAResourceResponse> createClearXAResourceResponse(ClearXAResourceResponse value) {
        return new JAXBElement<ClearXAResourceResponse>(_ClearXAResourceResponse_QNAME, ClearXAResourceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ClearXAResource }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.threadedserver.wsat.ws.ibm.com/", name = "clearXAResource")
    public JAXBElement<ClearXAResource> createClearXAResource(ClearXAResource value) {
        return new JAXBElement<ClearXAResource>(_ClearXAResource_QNAME, ClearXAResource.class, null, value);
    }

}
