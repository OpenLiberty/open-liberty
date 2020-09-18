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
package com.ibm.ws.wsatAppServiceOptional.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsatAppServiceOptional.client package. 
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

    private final static QName _SayHelloToOtherWithoutResponse_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "sayHelloToOtherWithoutResponse");
    private final static QName _SayHelloToOtherWithout_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "sayHelloToOtherWithout");
    private final static QName _SayHelloToOtherResponse_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "sayHelloToOtherResponse");
    private final static QName _SayHelloToOther_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "sayHelloToOther");
    private final static QName _SayHelloResponse_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "sayHelloResponse");
    private final static QName _SayHello_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "sayHello");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsatAppServiceOptional.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SayHelloToOther }
     * 
     */
    public SayHelloToOther createSayHelloToOther() {
        return new SayHelloToOther();
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
     * Create an instance of {@link SayHelloToOtherResponse }
     * 
     */
    public SayHelloToOtherResponse createSayHelloToOtherResponse() {
        return new SayHelloToOtherResponse();
    }
    
    /**
     * Create an instance of {@link SayHelloToOtherWithout }
     * 
     */
    public SayHelloToOtherWithout createSayHelloToOtherWithout() {
        return new SayHelloToOtherWithout();
    }
    
    /**
     * Create an instance of {@link SayHelloToOtherWithoutResponse }
     * 
     */
    public SayHelloToOtherWithoutResponse createSayHelloToOtherWithoutResponse() {
        return new SayHelloToOtherWithoutResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloToOtherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", name = "sayHelloToOtherWithoutResponse")
    public JAXBElement<SayHelloToOtherWithoutResponse> createSayHelloToOtherWithoutResponse(SayHelloToOtherWithoutResponse value) {
        return new JAXBElement<SayHelloToOtherWithoutResponse>(_SayHelloToOtherWithoutResponse_QNAME, SayHelloToOtherWithoutResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloToOther }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", name = "sayHelloToOtherWithout")
    public JAXBElement<SayHelloToOtherWithout> createSayHelloToOtherWithout(SayHelloToOtherWithout value) {
        return new JAXBElement<SayHelloToOtherWithout>(_SayHelloToOtherWithout_QNAME, SayHelloToOtherWithout.class, null, value);
    }
    
    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloToOtherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", name = "sayHelloToOtherResponse")
    public JAXBElement<SayHelloToOtherResponse> createSayHelloToOtherResponse(SayHelloToOtherResponse value) {
        return new JAXBElement<SayHelloToOtherResponse>(_SayHelloToOtherResponse_QNAME, SayHelloToOtherResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloToOther }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", name = "sayHelloToOther")
    public JAXBElement<SayHelloToOther> createSayHelloToOther(SayHelloToOther value) {
        return new JAXBElement<SayHelloToOther>(_SayHelloToOther_QNAME, SayHelloToOther.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHelloResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", name = "sayHelloResponse")
    public JAXBElement<SayHelloResponse> createSayHelloResponse(SayHelloResponse value) {
        return new JAXBElement<SayHelloResponse>(_SayHelloResponse_QNAME, SayHelloResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SayHello }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", name = "sayHello")
    public JAXBElement<SayHello> createSayHello(SayHello value) {
        return new JAXBElement<SayHello>(_SayHello_QNAME, SayHello.class, null, value);
    }

}
