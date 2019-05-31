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
package com.ibm.ws.liberty.test.wscontext.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.ws.liberty.test.wscontext.client package.
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

    private final static QName _IsServletContextNull_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isServletContextNull");
    private final static QName _GetServletContextParameterResponse_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "getServletContextParameterResponse");
    private final static QName _IsInjectionInstanceNullResponse_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isInjectionInstanceNullResponse");
    private final static QName _IsMessageContextNullResponse_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isMessageContextNullResponse");
    private final static QName _GetServletContextParameter_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "getServletContextParameter");
    private final static QName _IsInjectionInstanceNull_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isInjectionInstanceNull");
    private final static QName _IsMessageContextNull_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isMessageContextNull");
    private final static QName _IsServletContextNullResponse_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isServletContextNullResponse");
    private final static QName _IsSelfDefinedJndiLookupInstanceNullResponse_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isSelfDefinedJndiLookupInstanceNullResponse");
    private final static QName _IsSelfDefinedJndiLookupInstanceNull_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isSelfDefinedJndiLookupInstanceNull");
    private final static QName _IsDefaultJndiLookupInstanceNullResponse_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isDefaultJndiLookupInstanceNullResponse");
    private final static QName _IsDefaultJndiLookupInstanceNull_QNAME = new QName("http://wscontext.test.liberty.ws.ibm.com", "isDefaultJndiLookupInstanceNull");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.liberty.test.wscontext.client
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link IsDefaultJndiLookupInstanceNull }
     *
     */
    public IsDefaultJndiLookupInstanceNull createIsDefaultJndiLookupInstanceNull() {
        return new IsDefaultJndiLookupInstanceNull();
    }

    /**
     * Create an instance of {@link IsServletContextNullResponse }
     *
     */
    public IsServletContextNullResponse createIsServletContextNullResponse() {
        return new IsServletContextNullResponse();
    }

    /**
     * Create an instance of {@link IsInjectionInstanceNullResponse }
     *
     */
    public IsInjectionInstanceNullResponse createIsInjectionInstanceNullResponse() {
        return new IsInjectionInstanceNullResponse();
    }

    /**
     * Create an instance of {@link GetServletContextParameterResponse }
     *
     */
    public GetServletContextParameterResponse createGetServletContextParameterResponse() {
        return new GetServletContextParameterResponse();
    }

    /**
     * Create an instance of {@link GetServletContextParameter }
     *
     */
    public GetServletContextParameter createGetServletContextParameter() {
        return new GetServletContextParameter();
    }

    /**
     * Create an instance of {@link IsInjectionInstanceNull }
     *
     */
    public IsInjectionInstanceNull createIsInjectionInstanceNull() {
        return new IsInjectionInstanceNull();
    }

    /**
     * Create an instance of {@link IsMessageContextNull }
     *
     */
    public IsMessageContextNull createIsMessageContextNull() {
        return new IsMessageContextNull();
    }

    /**
     * Create an instance of {@link IsMessageContextNullResponse }
     *
     */
    public IsMessageContextNullResponse createIsMessageContextNullResponse() {
        return new IsMessageContextNullResponse();
    }

    /**
     * Create an instance of {@link IsDefaultJndiLookupInstanceNullResponse }
     *
     */
    public IsDefaultJndiLookupInstanceNullResponse createIsDefaultJndiLookupInstanceNullResponse() {
        return new IsDefaultJndiLookupInstanceNullResponse();
    }

    /**
     * Create an instance of {@link IsServletContextNull }
     *
     */
    public IsServletContextNull createIsServletContextNull() {
        return new IsServletContextNull();
    }

    /**
     * Create an instance of {@link IsSelfDefinedJndiLookupInstanceNullResponse }
     *
     */
    public IsSelfDefinedJndiLookupInstanceNullResponse createIsSelfDefinedJndiLookupInstanceNullResponse() {
        return new IsSelfDefinedJndiLookupInstanceNullResponse();
    }

    /**
     * Create an instance of {@link IsSelfDefinedJndiLookupInstanceNull }
     *
     */
    public IsSelfDefinedJndiLookupInstanceNull createIsSelfDefinedJndiLookupInstanceNull() {
        return new IsSelfDefinedJndiLookupInstanceNull();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsServletContextNull }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isServletContextNull")
    public JAXBElement<IsServletContextNull> createIsServletContextNull(IsServletContextNull value) {
        return new JAXBElement<IsServletContextNull>(_IsServletContextNull_QNAME, IsServletContextNull.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetServletContextParameterResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "getServletContextParameterResponse")
    public JAXBElement<GetServletContextParameterResponse> createGetServletContextParameterResponse(GetServletContextParameterResponse value) {
        return new JAXBElement<GetServletContextParameterResponse>(_GetServletContextParameterResponse_QNAME, GetServletContextParameterResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsInjectionInstanceNullResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isInjectionInstanceNullResponse")
    public JAXBElement<IsInjectionInstanceNullResponse> createIsInjectionInstanceNullResponse(IsInjectionInstanceNullResponse value) {
        return new JAXBElement<IsInjectionInstanceNullResponse>(_IsInjectionInstanceNullResponse_QNAME, IsInjectionInstanceNullResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsMessageContextNullResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isMessageContextNullResponse")
    public JAXBElement<IsMessageContextNullResponse> createIsMessageContextNullResponse(IsMessageContextNullResponse value) {
        return new JAXBElement<IsMessageContextNullResponse>(_IsMessageContextNullResponse_QNAME, IsMessageContextNullResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetServletContextParameter }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "getServletContextParameter")
    public JAXBElement<GetServletContextParameter> createGetServletContextParameter(GetServletContextParameter value) {
        return new JAXBElement<GetServletContextParameter>(_GetServletContextParameter_QNAME, GetServletContextParameter.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsInjectionInstanceNull }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isInjectionInstanceNull")
    public JAXBElement<IsInjectionInstanceNull> createIsInjectionInstanceNull(IsInjectionInstanceNull value) {
        return new JAXBElement<IsInjectionInstanceNull>(_IsInjectionInstanceNull_QNAME, IsInjectionInstanceNull.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsMessageContextNull }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isMessageContextNull")
    public JAXBElement<IsMessageContextNull> createIsMessageContextNull(IsMessageContextNull value) {
        return new JAXBElement<IsMessageContextNull>(_IsMessageContextNull_QNAME, IsMessageContextNull.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsServletContextNullResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isServletContextNullResponse")
    public JAXBElement<IsServletContextNullResponse> createIsServletContextNullResponse(IsServletContextNullResponse value) {
        return new JAXBElement<IsServletContextNullResponse>(_IsServletContextNullResponse_QNAME, IsServletContextNullResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsSelfDefinedJndiLookupInstanceNullResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isSelfDefinedJndiLookupInstanceNullResponse")
    public JAXBElement<IsSelfDefinedJndiLookupInstanceNullResponse> createIsSelfDefinedJndiLookupInstanceNullResponse(IsSelfDefinedJndiLookupInstanceNullResponse value) {
        return new JAXBElement<IsSelfDefinedJndiLookupInstanceNullResponse>(_IsSelfDefinedJndiLookupInstanceNullResponse_QNAME, IsSelfDefinedJndiLookupInstanceNullResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsSelfDefinedJndiLookupInstanceNull }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isSelfDefinedJndiLookupInstanceNull")
    public JAXBElement<IsSelfDefinedJndiLookupInstanceNull> createIsSelfDefinedJndiLookupInstanceNull(IsSelfDefinedJndiLookupInstanceNull value) {
        return new JAXBElement<IsSelfDefinedJndiLookupInstanceNull>(_IsSelfDefinedJndiLookupInstanceNull_QNAME, IsSelfDefinedJndiLookupInstanceNull.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsDefaultJndiLookupInstanceNullResponse }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isDefaultJndiLookupInstanceNullResponse")
    public JAXBElement<IsDefaultJndiLookupInstanceNullResponse> createIsDefaultJndiLookupInstanceNullResponse(IsDefaultJndiLookupInstanceNullResponse value) {
        return new JAXBElement<IsDefaultJndiLookupInstanceNullResponse>(_IsDefaultJndiLookupInstanceNullResponse_QNAME, IsDefaultJndiLookupInstanceNullResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsDefaultJndiLookupInstanceNull }{@code >}
     *
     */
    @XmlElementDecl(namespace = "http://wscontext.test.liberty.ws.ibm.com", name = "isDefaultJndiLookupInstanceNull")
    public JAXBElement<IsDefaultJndiLookupInstanceNull> createIsDefaultJndiLookupInstanceNull(IsDefaultJndiLookupInstanceNull value) {
        return new JAXBElement<IsDefaultJndiLookupInstanceNull>(_IsDefaultJndiLookupInstanceNull_QNAME, IsDefaultJndiLookupInstanceNull.class, null, value);
    }

}
