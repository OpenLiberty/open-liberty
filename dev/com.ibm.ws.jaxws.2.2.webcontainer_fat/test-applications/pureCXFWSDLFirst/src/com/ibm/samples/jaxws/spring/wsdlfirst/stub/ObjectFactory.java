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
package com.ibm.samples.jaxws.spring.wsdlfirst.stub;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.example.customerservice package.
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

    private final static QName _NoSuchCustomer_QNAME = new QName("http://customerservice.example.com/", "NoSuchCustomer");
    private final static QName _GetCustomersByNameResponse_QNAME = new QName("http://customerservice.example.com/", "getCustomersByNameResponse");
    private final static QName _GetCustomersByName_QNAME = new QName("http://customerservice.example.com/", "getCustomersByName");
    private final static QName _UpdateCustomer_QNAME = new QName("http://customerservice.example.com/", "updateCustomer");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.example.customerservice
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link UpdateCustomer }
     *
     */
    public UpdateCustomer createUpdateCustomer() {
        return new UpdateCustomer();
    }

    /**
     * Create an instance of {@link GetCustomersByNameResponse }
     *
     */
    public GetCustomersByNameResponse createGetCustomersByNameResponse() {
        return new GetCustomersByNameResponse();
    }

    /**
     * Create an instance of {@link Customer }
     *
     */
    public Customer createCustomer() {
        return new Customer();
    }

    /**
     * Create an instance of {@link GetCustomersByName }
     *
     */
    public GetCustomersByName createGetCustomersByName() {
        return new GetCustomersByName();
    }

    /**
     * Create an instance of {@link NoSuchCustomer }
     *
     */
    public NoSuchCustomer createNoSuchCustomer() {
        return new NoSuchCustomer();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NoSuchCustomer }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://customerservice.example.com/", name = "NoSuchCustomer")
    public JAXBElement<NoSuchCustomer> createNoSuchCustomer(NoSuchCustomer value) {
        return new JAXBElement<NoSuchCustomer>(_NoSuchCustomer_QNAME, NoSuchCustomer.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCustomersByNameResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://customerservice.example.com/", name = "getCustomersByNameResponse")
    public JAXBElement<GetCustomersByNameResponse> createGetCustomersByNameResponse(GetCustomersByNameResponse value) {
        return new JAXBElement<GetCustomersByNameResponse>(_GetCustomersByNameResponse_QNAME, GetCustomersByNameResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCustomersByName }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://customerservice.example.com/", name = "getCustomersByName")
    public JAXBElement<GetCustomersByName> createGetCustomersByName(GetCustomersByName value) {
        return new JAXBElement<GetCustomersByName>(_GetCustomersByName_QNAME, GetCustomersByName.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateCustomer }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://customerservice.example.com/", name = "updateCustomer")
    public JAXBElement<UpdateCustomer> createUpdateCustomer(UpdateCustomer value) {
        return new JAXBElement<UpdateCustomer>(_UpdateCustomer_QNAME, UpdateCustomer.class, null, value);
    }

}
