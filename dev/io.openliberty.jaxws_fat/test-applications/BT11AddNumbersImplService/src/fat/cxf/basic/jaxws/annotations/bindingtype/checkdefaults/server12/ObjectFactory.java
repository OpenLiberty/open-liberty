/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12 package. 
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

    private final static QName _AddTwoNumbersResponse_QNAME = new QName("http://server12.checkdefaults.bindingtype.annotations.jaxws.basic.cxf.fat/", "addTwoNumbersResponse");
    private final static QName _AddTwoNumbers_QNAME = new QName("http://server12.checkdefaults.bindingtype.annotations.jaxws.basic.cxf.fat/", "addTwoNumbers");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AddTwoNumbers }
     * 
     */
    public AddTwoNumbers createAddTwoNumbers() {
        return new AddTwoNumbers();
    }

    /**
     * Create an instance of {@link AddTwoNumbersResponse }
     * 
     */
    public AddTwoNumbersResponse createAddTwoNumbersResponse() {
        return new AddTwoNumbersResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddTwoNumbersResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server12.checkdefaults.bindingtype.annotations.jaxws.basic.cxf.fat/", name = "addTwoNumbersResponse")
    public JAXBElement<AddTwoNumbersResponse> createAddTwoNumbersResponse(AddTwoNumbersResponse value) {
        return new JAXBElement<AddTwoNumbersResponse>(_AddTwoNumbersResponse_QNAME, AddTwoNumbersResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddTwoNumbers }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server12.checkdefaults.bindingtype.annotations.jaxws.basic.cxf.fat/", name = "addTwoNumbers")
    public JAXBElement<AddTwoNumbers> createAddTwoNumbers(AddTwoNumbers value) {
        return new JAXBElement<AddTwoNumbers>(_AddTwoNumbers_QNAME, AddTwoNumbers.class, null, value);
    }

}
