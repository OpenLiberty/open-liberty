/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.jaxws.client package. 
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

    private final static QName _AddNumbers_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "addNumbers");
    private final static QName _AddNumbersResponse_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "addNumbersResponse");
    private final static QName _AddNegativesResponse_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "addNegativesResponse");
    private final static QName _AddNumbersException_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "AddNumbersException");
    private final static QName _AddNegatives_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "addNegatives");
    private final static QName _LocalName_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "LocalName");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AddNegativesResponse }
     * 
     */
    public AddNegativesResponse createAddNegativesResponse() {
        return new AddNegativesResponse();
    }

    /**
     * Create an instance of {@link AddNumbersResponse }
     * 
     */
    public AddNumbersResponse createAddNumbersResponse() {
        return new AddNumbersResponse();
    }

    /**
     * Create an instance of {@link LocalName }
     * 
     */
    public LocalName createLocalName() {
        return new LocalName();
    }

    /**
     * Create an instance of {@link AddNegatives }
     * 
     */
    public AddNegatives createAddNegatives() {
        return new AddNegatives();
    }

    /**
     * Create an instance of {@link AddNumbersException }
     * 
     */
    public AddNumbersException createAddNumbersException() {
        return new AddNumbersException();
    }

    /**
     * Create an instance of {@link AddNumbers_Type }
     * 
     */
    public AddNumbers_Type createAddNumbers_Type() {
        return new AddNumbers_Type();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNumbers_Type }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://provider.jaxws.ws.ibm.com/", name = "addNumbers")
    public JAXBElement<AddNumbers_Type> createAddNumbers(AddNumbers_Type value) {
        return new JAXBElement<AddNumbers_Type>(_AddNumbers_QNAME, AddNumbers_Type.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNumbersResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://provider.jaxws.ws.ibm.com/", name = "addNumbersResponse")
    public JAXBElement<AddNumbersResponse> createAddNumbersResponse(AddNumbersResponse value) {
        return new JAXBElement<AddNumbersResponse>(_AddNumbersResponse_QNAME, AddNumbersResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNegativesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://provider.jaxws.ws.ibm.com/", name = "addNegativesResponse")
    public JAXBElement<AddNegativesResponse> createAddNegativesResponse(AddNegativesResponse value) {
        return new JAXBElement<AddNegativesResponse>(_AddNegativesResponse_QNAME, AddNegativesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNumbersException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://provider.jaxws.ws.ibm.com/", name = "AddNumbersException")
    public JAXBElement<AddNumbersException> createAddNumbersException(AddNumbersException value) {
        return new JAXBElement<AddNumbersException>(_AddNumbersException_QNAME, AddNumbersException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNegatives }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://provider.jaxws.ws.ibm.com/", name = "addNegatives")
    public JAXBElement<AddNegatives> createAddNegatives(AddNegatives value) {
        return new JAXBElement<AddNegatives>(_AddNegatives_QNAME, AddNegatives.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LocalName }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://provider.jaxws.ws.ibm.com/", name = "LocalName")
    public JAXBElement<LocalName> createLocalName(LocalName value) {
        return new JAXBElement<LocalName>(_LocalName_QNAME, LocalName.class, null, value);
    }

}
