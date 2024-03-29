/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/



package com.ibm.ws.jaxws.ejbinwar.ejb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.jaxws.ejbinwar.ejb package. 
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

    private final static QName _SayHello_QNAME = new QName("http://ejb.ejbinwar.jaxws.ws.ibm.com/", "sayHello");
    private final static QName _InvokeOther_QNAME = new QName("http://ejb.ejbinwar.jaxws.ws.ibm.com/", "invokeOther");
    private final static QName _SayHelloResponse_QNAME = new QName("http://ejb.ejbinwar.jaxws.ws.ibm.com/", "sayHelloResponse");
    private final static QName _InvokeOtherResponse_QNAME = new QName("http://ejb.ejbinwar.jaxws.ws.ibm.com/", "invokeOtherResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.ejbinwar.ejb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SayHello_Type }
     * 
     */
    public SayHello_Type createSayHello_Type() {
        return new SayHello_Type();
    }

    /**
     * Create an instance of {@link InvokeOtherResponse }
     * 
     */
    public InvokeOtherResponse createInvokeOtherResponse() {
        return new InvokeOtherResponse();
    }

    /**
     * Create an instance of {@link InvokeOther }
     * 
     */
    public InvokeOther createInvokeOther() {
        return new InvokeOther();
    }

    /**
     * Create an instance of {@link SayHelloResponse }
     * 
     */
    public SayHelloResponse createSayHelloResponse() {
        return new SayHelloResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHello_Type }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ejb.ejbinwar.jaxws.ws.ibm.com/", name = "sayHello")
    public JAXBElement<SayHello_Type> createSayHello(SayHello_Type value) {
        return new JAXBElement<SayHello_Type>(_SayHello_QNAME, SayHello_Type.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeOther }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ejb.ejbinwar.jaxws.ws.ibm.com/", name = "invokeOther")
    public JAXBElement<InvokeOther> createInvokeOther(InvokeOther value) {
        return new JAXBElement<InvokeOther>(_InvokeOther_QNAME, InvokeOther.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ejb.ejbinwar.jaxws.ws.ibm.com/", name = "sayHelloResponse")
    public JAXBElement<SayHelloResponse> createSayHelloResponse(SayHelloResponse value) {
        return new JAXBElement<SayHelloResponse>(_SayHelloResponse_QNAME, SayHelloResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeOtherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ejb.ejbinwar.jaxws.ws.ibm.com/", name = "invokeOtherResponse")
    public JAXBElement<InvokeOtherResponse> createInvokeOtherResponse(InvokeOtherResponse value) {
        return new JAXBElement<InvokeOtherResponse>(_InvokeOtherResponse_QNAME, InvokeOtherResponse.class, null, value);
    }

}
