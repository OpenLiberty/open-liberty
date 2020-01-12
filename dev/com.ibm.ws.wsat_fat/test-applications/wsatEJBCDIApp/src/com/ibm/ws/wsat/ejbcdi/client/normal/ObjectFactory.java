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
package com.ibm.ws.wsat.ejbcdi.client.normal;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsat.ejbcdi.client.normal package. 
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

    private final static QName _NormalSayHelloToOtherResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "normalSayHelloToOtherResponse");
    private final static QName _NamingException_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "NamingException");
    private final static QName _SQLException_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "SQLException");
    private final static QName _NormalSayHelloToOther_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "normalSayHelloToOther");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsat.ejbcdi.client.normal
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link NormalSayHelloToOther }
     * 
     */
    public NormalSayHelloToOther createNormalSayHelloToOther() {
        return new NormalSayHelloToOther();
    }

    /**
     * Create an instance of {@link NamingException }
     * 
     */
    public NamingException createNamingException() {
        return new NamingException();
    }

    /**
     * Create an instance of {@link SQLException }
     * 
     */
    public SQLException createSQLException() {
        return new SQLException();
    }

    /**
     * Create an instance of {@link NormalSayHelloToOtherResponse }
     * 
     */
    public NormalSayHelloToOtherResponse createNormalSayHelloToOtherResponse() {
        return new NormalSayHelloToOtherResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NormalSayHelloToOtherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "normalSayHelloToOtherResponse")
    public JAXBElement<NormalSayHelloToOtherResponse> createNormalSayHelloToOtherResponse(NormalSayHelloToOtherResponse value) {
        return new JAXBElement<NormalSayHelloToOtherResponse>(_NormalSayHelloToOtherResponse_QNAME, NormalSayHelloToOtherResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NamingException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "NamingException")
    public JAXBElement<NamingException> createNamingException(NamingException value) {
        return new JAXBElement<NamingException>(_NamingException_QNAME, NamingException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SQLException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "SQLException")
    public JAXBElement<SQLException> createSQLException(SQLException value) {
        return new JAXBElement<SQLException>(_SQLException_QNAME, SQLException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NormalSayHelloToOther }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "normalSayHelloToOther")
    public JAXBElement<NormalSayHelloToOther> createNormalSayHelloToOther(NormalSayHelloToOther value) {
        return new JAXBElement<NormalSayHelloToOther>(_NormalSayHelloToOther_QNAME, NormalSayHelloToOther.class, null, value);
    }

}
