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
package com.ibm.ws.jaxws.namespacecheck.wsdl.importedserviceschema;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.ws.jaxws.namespacecheck.wsdl.importedserviceschema package.
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

    private static final QName _CFault_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "CFault");
    private static final QName _CFaultCode_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "CFaultCode");
    private static final QName _CFaultString_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "CFaultString");
    private static final QName _CHeader_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "CHeader");
    private static final QName _CPerson_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "CPerson");
    private static final QName _CustomHeader_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "CustomHeader");
    private static final QName _Name_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "Name");
    private static final QName _Email_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", "Email");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.namespacecheck.wsdl.importedserviceschema
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CustomFaultType }
     *
     */
    public CustomFaultType createCustomFaultType() {
        return new CustomFaultType();
    }

    /**
     * Create an instance of {@link CustomHeaderType }
     *
     */
    public CustomHeaderType createCustomHeaderType() {
        return new CustomHeaderType();
    }

    /**
     * Create an instance of {@link PersonType }
     *
     */
    public PersonType createPersonType() {
        return new PersonType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CustomFaultType }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link CustomFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "CFault")
    public JAXBElement<CustomFaultType> createCFault(CustomFaultType value) {
        return new JAXBElement<CustomFaultType>(_CFault_QNAME, CustomFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "CFaultCode")
    public JAXBElement<String> createCFaultCode(String value) {
        return new JAXBElement<String>(_CFaultCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "CFaultString")
    public JAXBElement<String> createCFaultString(String value) {
        return new JAXBElement<String>(_CFaultString_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CustomHeaderType }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link CustomHeaderType }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "CHeader")
    public JAXBElement<CustomHeaderType> createCHeader(CustomHeaderType value) {
        return new JAXBElement<CustomHeaderType>(_CHeader_QNAME, CustomHeaderType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PersonType }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link PersonType }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "CPerson")
    public JAXBElement<PersonType> createCPerson(PersonType value) {
        return new JAXBElement<PersonType>(_CPerson_QNAME, PersonType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CustomHeaderType }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link CustomHeaderType }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "CustomHeader")
    public JAXBElement<CustomHeaderType> createCustomHeader(CustomHeaderType value) {
        return new JAXBElement<CustomHeaderType>(_CustomHeader_QNAME, CustomHeaderType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "Name")
    public JAXBElement<String> createName(String value) {
        return new JAXBElement<String>(_Name_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd", name = "Email")
    public JAXBElement<String> createEmail(String value) {
        return new JAXBElement<String>(_Email_QNAME, String.class, null, value);
    }

}
