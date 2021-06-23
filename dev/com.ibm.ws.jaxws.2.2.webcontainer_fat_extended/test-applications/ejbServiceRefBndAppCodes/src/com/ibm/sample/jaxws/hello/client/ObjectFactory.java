/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sample.jaxws.hello.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.sample.jaxws.hello.client package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _HelloResponse_QNAME = new QName("http://hello.jaxws.sample.ibm.com/", "helloResponse");
    private final static QName _Hello_QNAME = new QName("http://hello.jaxws.sample.ibm.com/", "hello");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.sample.jaxws.hello.client
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link Hello_Type }
     *
     */
    public Hello_Type createHello_Type() {
        return new Hello_Type();
    }

    /**
     * Create an instance of {@link HelloResponse }
     *
     */
    public HelloResponse createHelloResponse() {
        return new HelloResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HelloResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://hello.jaxws.sample.ibm.com/", name = "helloResponse")
    public JAXBElement<HelloResponse> createHelloResponse(HelloResponse value) {
        return new JAXBElement<HelloResponse>(_HelloResponse_QNAME, HelloResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Hello_Type }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://hello.jaxws.sample.ibm.com/", name = "hello")
    public JAXBElement<Hello_Type> createHello(Hello_Type value) {
        return new JAXBElement<Hello_Type>(_Hello_QNAME, Hello_Type.class, null, value);
    }

}
