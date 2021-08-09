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
package com.ibm.ws.wsat.ejbcdi.client.cdi;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsat.ejbcdi.client.cdi package. 
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

    private final static QName _TestCDISayHelloToOtherWithNeverResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithNeverResponse");
    private final static QName _TestCDISayHelloToOtherWithNotSupported_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithNotSupported");
    private final static QName _TestCDISayHelloToOtherWithRequiresNew_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithRequiresNew");
    private final static QName _TestCDISayHelloToOther_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOther");
    private final static QName _TestCDISayHelloToOtherWithMandatory_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithMandatory");
    private final static QName _TestCDISayHelloToOtherWithMandatoryResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithMandatoryResponse");
    private final static QName _SQLException_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "SQLException");
    private final static QName _TestCDISayHelloToOtherWithNotSupportedResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithNotSupportedResponse");
    private final static QName _NamingException_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "NamingException");
    private final static QName _TestCDISayHelloToOtherWithSupportsResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithSupportsResponse");
    private final static QName _TestCDISayHelloToOtherResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherResponse");
    private final static QName _TestCDISayHelloToOtherWithSupports_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithSupports");
    private final static QName _TestCDISayHelloToOtherWithRequiresNewResponse_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithRequiresNewResponse");
    private final static QName _TestCDISayHelloToOtherWithNever_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "testCDISayHelloToOtherWithNever");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsat.ejbcdi.client.cdi
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithNotSupported }
     * 
     */
    public TestCDISayHelloToOtherWithNotSupported createTestCDISayHelloToOtherWithNotSupported() {
        return new TestCDISayHelloToOtherWithNotSupported();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithRequiresNew }
     * 
     */
    public TestCDISayHelloToOtherWithRequiresNew createTestCDISayHelloToOtherWithRequiresNew() {
        return new TestCDISayHelloToOtherWithRequiresNew();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithNotSupportedResponse }
     * 
     */
    public TestCDISayHelloToOtherWithNotSupportedResponse createTestCDISayHelloToOtherWithNotSupportedResponse() {
        return new TestCDISayHelloToOtherWithNotSupportedResponse();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithRequiresNewResponse }
     * 
     */
    public TestCDISayHelloToOtherWithRequiresNewResponse createTestCDISayHelloToOtherWithRequiresNewResponse() {
        return new TestCDISayHelloToOtherWithRequiresNewResponse();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithMandatory }
     * 
     */
    public TestCDISayHelloToOtherWithMandatory createTestCDISayHelloToOtherWithMandatory() {
        return new TestCDISayHelloToOtherWithMandatory();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithSupportsResponse }
     * 
     */
    public TestCDISayHelloToOtherWithSupportsResponse createTestCDISayHelloToOtherWithSupportsResponse() {
        return new TestCDISayHelloToOtherWithSupportsResponse();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithNever }
     * 
     */
    public TestCDISayHelloToOtherWithNever createTestCDISayHelloToOtherWithNever() {
        return new TestCDISayHelloToOtherWithNever();
    }

    /**
     * Create an instance of {@link NamingException }
     * 
     */
    public NamingException createNamingException() {
        return new NamingException();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherResponse }
     * 
     */
    public TestCDISayHelloToOtherResponse createTestCDISayHelloToOtherResponse() {
        return new TestCDISayHelloToOtherResponse();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithMandatoryResponse }
     * 
     */
    public TestCDISayHelloToOtherWithMandatoryResponse createTestCDISayHelloToOtherWithMandatoryResponse() {
        return new TestCDISayHelloToOtherWithMandatoryResponse();
    }

    /**
     * Create an instance of {@link SQLException }
     * 
     */
    public SQLException createSQLException() {
        return new SQLException();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithNeverResponse }
     * 
     */
    public TestCDISayHelloToOtherWithNeverResponse createTestCDISayHelloToOtherWithNeverResponse() {
        return new TestCDISayHelloToOtherWithNeverResponse();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOther }
     * 
     */
    public TestCDISayHelloToOther createTestCDISayHelloToOther() {
        return new TestCDISayHelloToOther();
    }

    /**
     * Create an instance of {@link TestCDISayHelloToOtherWithSupports }
     * 
     */
    public TestCDISayHelloToOtherWithSupports createTestCDISayHelloToOtherWithSupports() {
        return new TestCDISayHelloToOtherWithSupports();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithNeverResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithNeverResponse")
    public JAXBElement<TestCDISayHelloToOtherWithNeverResponse> createTestCDISayHelloToOtherWithNeverResponse(TestCDISayHelloToOtherWithNeverResponse value) {
        return new JAXBElement<TestCDISayHelloToOtherWithNeverResponse>(_TestCDISayHelloToOtherWithNeverResponse_QNAME, TestCDISayHelloToOtherWithNeverResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithNotSupported }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithNotSupported")
    public JAXBElement<TestCDISayHelloToOtherWithNotSupported> createTestCDISayHelloToOtherWithNotSupported(TestCDISayHelloToOtherWithNotSupported value) {
        return new JAXBElement<TestCDISayHelloToOtherWithNotSupported>(_TestCDISayHelloToOtherWithNotSupported_QNAME, TestCDISayHelloToOtherWithNotSupported.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithRequiresNew }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithRequiresNew")
    public JAXBElement<TestCDISayHelloToOtherWithRequiresNew> createTestCDISayHelloToOtherWithRequiresNew(TestCDISayHelloToOtherWithRequiresNew value) {
        return new JAXBElement<TestCDISayHelloToOtherWithRequiresNew>(_TestCDISayHelloToOtherWithRequiresNew_QNAME, TestCDISayHelloToOtherWithRequiresNew.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOther }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOther")
    public JAXBElement<TestCDISayHelloToOther> createTestCDISayHelloToOther(TestCDISayHelloToOther value) {
        return new JAXBElement<TestCDISayHelloToOther>(_TestCDISayHelloToOther_QNAME, TestCDISayHelloToOther.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithMandatory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithMandatory")
    public JAXBElement<TestCDISayHelloToOtherWithMandatory> createTestCDISayHelloToOtherWithMandatory(TestCDISayHelloToOtherWithMandatory value) {
        return new JAXBElement<TestCDISayHelloToOtherWithMandatory>(_TestCDISayHelloToOtherWithMandatory_QNAME, TestCDISayHelloToOtherWithMandatory.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithMandatoryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithMandatoryResponse")
    public JAXBElement<TestCDISayHelloToOtherWithMandatoryResponse> createTestCDISayHelloToOtherWithMandatoryResponse(TestCDISayHelloToOtherWithMandatoryResponse value) {
        return new JAXBElement<TestCDISayHelloToOtherWithMandatoryResponse>(_TestCDISayHelloToOtherWithMandatoryResponse_QNAME, TestCDISayHelloToOtherWithMandatoryResponse.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithNotSupportedResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithNotSupportedResponse")
    public JAXBElement<TestCDISayHelloToOtherWithNotSupportedResponse> createTestCDISayHelloToOtherWithNotSupportedResponse(TestCDISayHelloToOtherWithNotSupportedResponse value) {
        return new JAXBElement<TestCDISayHelloToOtherWithNotSupportedResponse>(_TestCDISayHelloToOtherWithNotSupportedResponse_QNAME, TestCDISayHelloToOtherWithNotSupportedResponse.class, null, value);
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
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithSupportsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithSupportsResponse")
    public JAXBElement<TestCDISayHelloToOtherWithSupportsResponse> createTestCDISayHelloToOtherWithSupportsResponse(TestCDISayHelloToOtherWithSupportsResponse value) {
        return new JAXBElement<TestCDISayHelloToOtherWithSupportsResponse>(_TestCDISayHelloToOtherWithSupportsResponse_QNAME, TestCDISayHelloToOtherWithSupportsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherResponse")
    public JAXBElement<TestCDISayHelloToOtherResponse> createTestCDISayHelloToOtherResponse(TestCDISayHelloToOtherResponse value) {
        return new JAXBElement<TestCDISayHelloToOtherResponse>(_TestCDISayHelloToOtherResponse_QNAME, TestCDISayHelloToOtherResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithSupports }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithSupports")
    public JAXBElement<TestCDISayHelloToOtherWithSupports> createTestCDISayHelloToOtherWithSupports(TestCDISayHelloToOtherWithSupports value) {
        return new JAXBElement<TestCDISayHelloToOtherWithSupports>(_TestCDISayHelloToOtherWithSupports_QNAME, TestCDISayHelloToOtherWithSupports.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithRequiresNewResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithRequiresNewResponse")
    public JAXBElement<TestCDISayHelloToOtherWithRequiresNewResponse> createTestCDISayHelloToOtherWithRequiresNewResponse(TestCDISayHelloToOtherWithRequiresNewResponse value) {
        return new JAXBElement<TestCDISayHelloToOtherWithRequiresNewResponse>(_TestCDISayHelloToOtherWithRequiresNewResponse_QNAME, TestCDISayHelloToOtherWithRequiresNewResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TestCDISayHelloToOtherWithNever }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.ejbcdi.wsat.ws.ibm.com/", name = "testCDISayHelloToOtherWithNever")
    public JAXBElement<TestCDISayHelloToOtherWithNever> createTestCDISayHelloToOtherWithNever(TestCDISayHelloToOtherWithNever value) {
        return new JAXBElement<TestCDISayHelloToOtherWithNever>(_TestCDISayHelloToOtherWithNever_QNAME, TestCDISayHelloToOtherWithNever.class, null, value);
    }

}
