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
package com.ibm.ws.wsat.ejbcdi.client.ejb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsat.ejbcdi.client.ejb package. 
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

    private final static QName _NamingException_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "NamingException");
    private final static QName _TestEJBSayHelloToOtherResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherResponse");
    private final static QName _TestEJBSayHelloToOtherWithNever_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithNever");
    private final static QName _TestEJBSayHelloToOtherWithRequiresNew_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithRequiresNew");
    private final static QName _TestEJBSayHelloToOtherWithNotSupportedResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithNotSupportedResponse");
    private final static QName _TestEJBSayHelloToOtherWithNotSupported_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithNotSupported");
    private final static QName _TestEJBSayHelloToOther_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOther");
    private final static QName _TestEJBSayHelloToOtherWithMandatory_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithMandatory");
    private final static QName _TestEJBSayHelloToOtherWithRequiresNewResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithRequiresNewResponse");
    private final static QName _TestEJBSayHelloToOtherWithSupports_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithSupports");
    private final static QName _SQLException_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "SQLException");
    private final static QName _TestEJBSayHelloToOtherWithSupportsResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithSupportsResponse");
    private final static QName _TestEJBSayHelloToOtherWithNeverResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithNeverResponse");
    private final static QName _TestEJBSayHelloToOtherWithMandatoryResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testEJBSayHelloToOtherWithMandatoryResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsat.ejbcdi.client.ejb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithMandatory }
     * 
     */
    public TestEJBSayHelloToOtherWithMandatory createTestEJBSayHelloToOtherWithMandatory() {
        return new TestEJBSayHelloToOtherWithMandatory();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithNotSupportedResponse }
     * 
     */
    public TestEJBSayHelloToOtherWithNotSupportedResponse createTestEJBSayHelloToOtherWithNotSupportedResponse() {
        return new TestEJBSayHelloToOtherWithNotSupportedResponse();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithMandatoryResponse }
     * 
     */
    public TestEJBSayHelloToOtherWithMandatoryResponse createTestEJBSayHelloToOtherWithMandatoryResponse() {
        return new TestEJBSayHelloToOtherWithMandatoryResponse();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithRequiresNew }
     * 
     */
    public TestEJBSayHelloToOtherWithRequiresNew createTestEJBSayHelloToOtherWithRequiresNew() {
        return new TestEJBSayHelloToOtherWithRequiresNew();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithRequiresNewResponse }
     * 
     */
    public TestEJBSayHelloToOtherWithRequiresNewResponse createTestEJBSayHelloToOtherWithRequiresNewResponse() {
        return new TestEJBSayHelloToOtherWithRequiresNewResponse();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithNotSupported }
     * 
     */
    public TestEJBSayHelloToOtherWithNotSupported createTestEJBSayHelloToOtherWithNotSupported() {
        return new TestEJBSayHelloToOtherWithNotSupported();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithSupports }
     * 
     */
    public TestEJBSayHelloToOtherWithSupports createTestEJBSayHelloToOtherWithSupports() {
        return new TestEJBSayHelloToOtherWithSupports();
    }

    /**
     * Create an instance of {@link NamingException }
     * 
     */
    public NamingException createNamingException() {
        return new NamingException();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherResponse }
     * 
     */
    public TestEJBSayHelloToOtherResponse createTestEJBSayHelloToOtherResponse() {
        return new TestEJBSayHelloToOtherResponse();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithSupportsResponse }
     * 
     */
    public TestEJBSayHelloToOtherWithSupportsResponse createTestEJBSayHelloToOtherWithSupportsResponse() {
        return new TestEJBSayHelloToOtherWithSupportsResponse();
    }

    /**
     * Create an instance of {@link SQLException }
     * 
     */
    public SQLException createSQLException() {
        return new SQLException();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithNeverResponse }
     * 
     */
    public TestEJBSayHelloToOtherWithNeverResponse createTestEJBSayHelloToOtherWithNeverResponse() {
        return new TestEJBSayHelloToOtherWithNeverResponse();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOther }
     * 
     */
    public TestEJBSayHelloToOther createTestEJBSayHelloToOther() {
        return new TestEJBSayHelloToOther();
    }

    /**
     * Create an instance of {@link TestEJBSayHelloToOtherWithNever }
     * 
     */
    public TestEJBSayHelloToOtherWithNever createTestEJBSayHelloToOtherWithNever() {
        return new TestEJBSayHelloToOtherWithNever();
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
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherResponse")
    public JAXBElement<TestEJBSayHelloToOtherResponse> createTestEJBSayHelloToOtherResponse(TestEJBSayHelloToOtherResponse value) {
        return new JAXBElement<TestEJBSayHelloToOtherResponse>(_TestEJBSayHelloToOtherResponse_QNAME, TestEJBSayHelloToOtherResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithNever }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithNever")
    public JAXBElement<TestEJBSayHelloToOtherWithNever> createTestEJBSayHelloToOtherWithNever(TestEJBSayHelloToOtherWithNever value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithNever>(_TestEJBSayHelloToOtherWithNever_QNAME, TestEJBSayHelloToOtherWithNever.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithRequiresNew }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithRequiresNew")
    public JAXBElement<TestEJBSayHelloToOtherWithRequiresNew> createTestEJBSayHelloToOtherWithRequiresNew(TestEJBSayHelloToOtherWithRequiresNew value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithRequiresNew>(_TestEJBSayHelloToOtherWithRequiresNew_QNAME, TestEJBSayHelloToOtherWithRequiresNew.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithNotSupportedResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithNotSupportedResponse")
    public JAXBElement<TestEJBSayHelloToOtherWithNotSupportedResponse> createTestEJBSayHelloToOtherWithNotSupportedResponse(TestEJBSayHelloToOtherWithNotSupportedResponse value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithNotSupportedResponse>(_TestEJBSayHelloToOtherWithNotSupportedResponse_QNAME, TestEJBSayHelloToOtherWithNotSupportedResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithNotSupported }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithNotSupported")
    public JAXBElement<TestEJBSayHelloToOtherWithNotSupported> createTestEJBSayHelloToOtherWithNotSupported(TestEJBSayHelloToOtherWithNotSupported value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithNotSupported>(_TestEJBSayHelloToOtherWithNotSupported_QNAME, TestEJBSayHelloToOtherWithNotSupported.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOther }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOther")
    public JAXBElement<TestEJBSayHelloToOther> createTestEJBSayHelloToOther(TestEJBSayHelloToOther value) {
        return new JAXBElement<TestEJBSayHelloToOther>(_TestEJBSayHelloToOther_QNAME, TestEJBSayHelloToOther.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithMandatory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithMandatory")
    public JAXBElement<TestEJBSayHelloToOtherWithMandatory> createTestEJBSayHelloToOtherWithMandatory(TestEJBSayHelloToOtherWithMandatory value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithMandatory>(_TestEJBSayHelloToOtherWithMandatory_QNAME, TestEJBSayHelloToOtherWithMandatory.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithRequiresNewResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithRequiresNewResponse")
    public JAXBElement<TestEJBSayHelloToOtherWithRequiresNewResponse> createTestEJBSayHelloToOtherWithRequiresNewResponse(TestEJBSayHelloToOtherWithRequiresNewResponse value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithRequiresNewResponse>(_TestEJBSayHelloToOtherWithRequiresNewResponse_QNAME, TestEJBSayHelloToOtherWithRequiresNewResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithSupports }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithSupports")
    public JAXBElement<TestEJBSayHelloToOtherWithSupports> createTestEJBSayHelloToOtherWithSupports(TestEJBSayHelloToOtherWithSupports value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithSupports>(_TestEJBSayHelloToOtherWithSupports_QNAME, TestEJBSayHelloToOtherWithSupports.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithSupportsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithSupportsResponse")
    public JAXBElement<TestEJBSayHelloToOtherWithSupportsResponse> createTestEJBSayHelloToOtherWithSupportsResponse(TestEJBSayHelloToOtherWithSupportsResponse value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithSupportsResponse>(_TestEJBSayHelloToOtherWithSupportsResponse_QNAME, TestEJBSayHelloToOtherWithSupportsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithNeverResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithNeverResponse")
    public JAXBElement<TestEJBSayHelloToOtherWithNeverResponse> createTestEJBSayHelloToOtherWithNeverResponse(TestEJBSayHelloToOtherWithNeverResponse value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithNeverResponse>(_TestEJBSayHelloToOtherWithNeverResponse_QNAME, TestEJBSayHelloToOtherWithNeverResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestEJBSayHelloToOtherWithMandatoryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testEJBSayHelloToOtherWithMandatoryResponse")
    public JAXBElement<TestEJBSayHelloToOtherWithMandatoryResponse> createTestEJBSayHelloToOtherWithMandatoryResponse(TestEJBSayHelloToOtherWithMandatoryResponse value) {
        return new JAXBElement<TestEJBSayHelloToOtherWithMandatoryResponse>(_TestEJBSayHelloToOtherWithMandatoryResponse_QNAME, TestEJBSayHelloToOtherWithMandatoryResponse.class, null, value);
    }

}
