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


package com.ibm.ws.jaxws.ejbjndi.web.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.jaxws.ejbjndi.web.client package. 
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

    private final static QName _Take_QNAME = new QName("http://web.ejbjndi.jaxws.ws.ibm.com/", "take");
    private final static QName _TakeResponse_QNAME = new QName("http://web.ejbjndi.jaxws.ws.ibm.com/", "takeResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.ejbjndi.web.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TakeResponse }
     * 
     */
    public TakeResponse createTakeResponse() {
        return new TakeResponse();
    }

    /**
     * Create an instance of {@link Take }
     * 
     */
    public Take createTake() {
        return new Take();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Take }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://web.ejbjndi.jaxws.ws.ibm.com/", name = "take")
    public JAXBElement<Take> createTake(Take value) {
        return new JAXBElement<Take>(_Take_QNAME, Take.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TakeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://web.ejbjndi.jaxws.ws.ibm.com/", name = "takeResponse")
    public JAXBElement<TakeResponse> createTakeResponse(TakeResponse value) {
        return new JAXBElement<TakeResponse>(_TakeResponse_QNAME, TakeResponse.class, null, value);
    }

}
