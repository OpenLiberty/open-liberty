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
package com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema;

import java.math.BigInteger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema package.
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

    private static final QName _NameSpaceCheck_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", "NameSpaceCheck");
    private static final QName _NameSpaceCheckResponse_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", "NameSpaceCheckResponse");
    private static final QName _DummyObject_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", "Dummy_Object");
    private static final QName _ID_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", "ID");
    private static final QName _DESCRIPTION_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", "DESCRIPTION");
    private static final QName _ResponseMessage_QNAME = new QName("http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", "ResponseMessage");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link NameSpaceCheckMessage }
     *
     */
    public NameSpaceCheckMessage createNameSpaceCheckMessage() {
        return new NameSpaceCheckMessage();
    }

    /**
     * Create an instance of {@link NameSpaceCheckResponseMessage }
     *
     */
    public NameSpaceCheckResponseMessage createNameSpaceCheckResponseMessage() {
        return new NameSpaceCheckResponseMessage();
    }

    /**
     * Create an instance of {@link DummyObjectTYPE }
     *
     */
    public DummyObjectTYPE createDummyObjectTYPE() {
        return new DummyObjectTYPE();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NameSpaceCheckMessage }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link NameSpaceCheckMessage }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", name = "NameSpaceCheck")
    public JAXBElement<NameSpaceCheckMessage> createNameSpaceCheck(NameSpaceCheckMessage value) {
        return new JAXBElement<NameSpaceCheckMessage>(_NameSpaceCheck_QNAME, NameSpaceCheckMessage.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NameSpaceCheckResponseMessage }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link NameSpaceCheckResponseMessage }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", name = "NameSpaceCheckResponse")
    public JAXBElement<NameSpaceCheckResponseMessage> createNameSpaceCheckResponse(NameSpaceCheckResponseMessage value) {
        return new JAXBElement<NameSpaceCheckResponseMessage>(_NameSpaceCheckResponse_QNAME, NameSpaceCheckResponseMessage.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DummyObjectTYPE }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link DummyObjectTYPE }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", name = "Dummy_Object")
    public JAXBElement<DummyObjectTYPE> createDummyObject(DummyObjectTYPE value) {
        return new JAXBElement<DummyObjectTYPE>(_DummyObject_QNAME, DummyObjectTYPE.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", name = "ID")
    public JAXBElement<BigInteger> createID(BigInteger value) {
        return new JAXBElement<BigInteger>(_ID_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", name = "DESCRIPTION")
    public JAXBElement<String> createDESCRIPTION(String value) {
        return new JAXBElement<String>(_DESCRIPTION_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     *
     * @param value
     *                  Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd", name = "ResponseMessage")
    public JAXBElement<String> createResponseMessage(String value) {
        return new JAXBElement<String>(_ResponseMessage_QNAME, String.class, null, value);
    }

}
