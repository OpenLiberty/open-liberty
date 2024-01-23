/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi.beanvalidation;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.ws.jaxws.cdi.beanvalidation package.
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

    private final static QName _OneWayWithValidation_QNAME = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "oneWayWithValidation");
    private final static QName _TwoWayWithValidation_QNAME = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "twoWayWithValidation");
    private final static QName _TwoWayWithValidationResponse_QNAME = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "twoWayWithValidationResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.cdi.beanvalidation
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link OneWayWithValidation }
     *
     */
    public OneWayWithValidation createOneWayWithValidation() {
        return new OneWayWithValidation();
    }

    /**
     * Create an instance of {@link TwoWayWithValidation }
     *
     */
    public TwoWayWithValidation createTwoWayWithValidation() {
        return new TwoWayWithValidation();
    }

    /**
     * Create an instance of {@link TwoWayWithValidationResponse }
     *
     */
    public TwoWayWithValidationResponse createTwoWayWithValidationResponse() {
        return new TwoWayWithValidationResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link OneWayWithValidation }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://beanvalidation.cdi.jaxws.ws.ibm.com/", name = "oneWayWithValidation")
    public JAXBElement<OneWayWithValidation> createOneWayWithValidation(OneWayWithValidation value) {
        return new JAXBElement<OneWayWithValidation>(_OneWayWithValidation_QNAME, OneWayWithValidation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TwoWayWithValidation }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://beanvalidation.cdi.jaxws.ws.ibm.com/", name = "twoWayWithValidation")
    public JAXBElement<TwoWayWithValidation> createTwoWayWithValidation(TwoWayWithValidation value) {
        return new JAXBElement<TwoWayWithValidation>(_TwoWayWithValidation_QNAME, TwoWayWithValidation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TwoWayWithValidationResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://beanvalidation.cdi.jaxws.ws.ibm.com/", name = "twoWayWithValidationResponse")
    public JAXBElement<TwoWayWithValidationResponse> createTwoWayWithValidationResponse(TwoWayWithValidationResponse value) {
        return new JAXBElement<TwoWayWithValidationResponse>(_TwoWayWithValidationResponse_QNAME, TwoWayWithValidationResponse.class, null, value);
    }

}
