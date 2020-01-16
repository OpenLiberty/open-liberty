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
package com.ibm.ws.wsat.endtoend.client.endtoend;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsat.endtoend.client.endtoend package. 
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

    private final static QName _CallAnotherResponse_QNAME = new QName("http://server.endtoend.wsat.ws.ibm.com/", "callAnotherResponse");
    private final static QName _SayHello_QNAME = new QName("http://server.endtoend.wsat.ws.ibm.com/", "sayHello");
    private final static QName _SayHelloResponse_QNAME = new QName("http://server.endtoend.wsat.ws.ibm.com/", "sayHelloResponse");
    private final static QName _CallAnother_QNAME = new QName("http://server.endtoend.wsat.ws.ibm.com/", "callAnother");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsat.endtoend.client.endtoend
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CallAnotherResponse }
     * 
     */
    public CallAnotherResponse createCallAnotherResponse() {
        return new CallAnotherResponse();
    }

    /**
     * Create an instance of {@link CallAnother }
     * 
     */
    public CallAnother createCallAnother() {
        return new CallAnother();
    }

    /**
     * Create an instance of {@link SayHelloResponse }
     * 
     */
    public SayHelloResponse createSayHelloResponse() {
        return new SayHelloResponse();
    }

    /**
     * Create an instance of {@link SayHello }
     * 
     */
    public SayHello createSayHello() {
        return new SayHello();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CallAnotherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.endtoend.wsat.ws.ibm.com/", name = "callAnotherResponse")
    public JAXBElement<CallAnotherResponse> createCallAnotherResponse(CallAnotherResponse value) {
        return new JAXBElement<CallAnotherResponse>(_CallAnotherResponse_QNAME, CallAnotherResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHello }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.endtoend.wsat.ws.ibm.com/", name = "sayHello")
    public JAXBElement<SayHello> createSayHello(SayHello value) {
        return new JAXBElement<SayHello>(_SayHello_QNAME, SayHello.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.endtoend.wsat.ws.ibm.com/", name = "sayHelloResponse")
    public JAXBElement<SayHelloResponse> createSayHelloResponse(SayHelloResponse value) {
        return new JAXBElement<SayHelloResponse>(_SayHelloResponse_QNAME, SayHelloResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CallAnother }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.endtoend.wsat.ws.ibm.com/", name = "callAnother")
    public JAXBElement<CallAnother> createCallAnother(CallAnother value) {
        return new JAXBElement<CallAnother>(_CallAnother_QNAME, CallAnother.class, null, value);
    }

}
