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
package com.ibm.ws.wsat.simpleclient.client.simple;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ibm.ws.wsat.simpleclient.client.simple package. 
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

    private final static QName _EnlistOneXAResource_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "enlistOneXAResource");
    private final static QName _Echo_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "echo");
    private final static QName _EnlistTwoXAResourcesResponse_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "enlistTwoXAResourcesResponse");
    private final static QName _EnlistOneXAResourceResponse_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "enlistOneXAResourceResponse");
    private final static QName _GetStatusResponse_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "getStatusResponse");
    private final static QName _EchoResponse_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "echoResponse");
    private final static QName _SleepResponse_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "sleepResponse");
    private final static QName _EnlistTwoXAResources_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "enlistTwoXAResources");
    private final static QName _GetStatus_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "getStatus");
    private final static QName _Sleep_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "sleep");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.wsat.simpleclient.client.simple
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetStatus }
     * 
     */
    public GetStatus createGetStatus() {
        return new GetStatus();
    }

    /**
     * Create an instance of {@link EnlistOneXAResource }
     * 
     */
    public EnlistOneXAResource createEnlistOneXAResource() {
        return new EnlistOneXAResource();
    }

    /**
     * Create an instance of {@link GetStatusResponse }
     * 
     */
    public GetStatusResponse createGetStatusResponse() {
        return new GetStatusResponse();
    }

    /**
     * Create an instance of {@link SleepResponse }
     * 
     */
    public SleepResponse createSleepResponse() {
        return new SleepResponse();
    }

    /**
     * Create an instance of {@link EnlistTwoXAResourcesResponse }
     * 
     */
    public EnlistTwoXAResourcesResponse createEnlistTwoXAResourcesResponse() {
        return new EnlistTwoXAResourcesResponse();
    }

    /**
     * Create an instance of {@link EnlistTwoXAResources }
     * 
     */
    public EnlistTwoXAResources createEnlistTwoXAResources() {
        return new EnlistTwoXAResources();
    }

    /**
     * Create an instance of {@link EnlistOneXAResourceResponse }
     * 
     */
    public EnlistOneXAResourceResponse createEnlistOneXAResourceResponse() {
        return new EnlistOneXAResourceResponse();
    }

    /**
     * Create an instance of {@link Sleep }
     * 
     */
    public Sleep createSleep() {
        return new Sleep();
    }

    /**
     * Create an instance of {@link EchoResponse }
     * 
     */
    public EchoResponse createEchoResponse() {
        return new EchoResponse();
    }

    /**
     * Create an instance of {@link Echo }
     * 
     */
    public Echo createEcho() {
        return new Echo();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnlistOneXAResource }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "enlistOneXAResource")
    public JAXBElement<EnlistOneXAResource> createEnlistOneXAResource(EnlistOneXAResource value) {
        return new JAXBElement<EnlistOneXAResource>(_EnlistOneXAResource_QNAME, EnlistOneXAResource.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Echo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "echo")
    public JAXBElement<Echo> createEcho(Echo value) {
        return new JAXBElement<Echo>(_Echo_QNAME, Echo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnlistTwoXAResourcesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "enlistTwoXAResourcesResponse")
    public JAXBElement<EnlistTwoXAResourcesResponse> createEnlistTwoXAResourcesResponse(EnlistTwoXAResourcesResponse value) {
        return new JAXBElement<EnlistTwoXAResourcesResponse>(_EnlistTwoXAResourcesResponse_QNAME, EnlistTwoXAResourcesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnlistOneXAResourceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "enlistOneXAResourceResponse")
    public JAXBElement<EnlistOneXAResourceResponse> createEnlistOneXAResourceResponse(EnlistOneXAResourceResponse value) {
        return new JAXBElement<EnlistOneXAResourceResponse>(_EnlistOneXAResourceResponse_QNAME, EnlistOneXAResourceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatusResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "getStatusResponse")
    public JAXBElement<GetStatusResponse> createGetStatusResponse(GetStatusResponse value) {
        return new JAXBElement<GetStatusResponse>(_GetStatusResponse_QNAME, GetStatusResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "echoResponse")
    public JAXBElement<EchoResponse> createEchoResponse(EchoResponse value) {
        return new JAXBElement<EchoResponse>(_EchoResponse_QNAME, EchoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SleepResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "sleepResponse")
    public JAXBElement<SleepResponse> createSleepResponse(SleepResponse value) {
        return new JAXBElement<SleepResponse>(_SleepResponse_QNAME, SleepResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnlistTwoXAResources }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "enlistTwoXAResources")
    public JAXBElement<EnlistTwoXAResources> createEnlistTwoXAResources(EnlistTwoXAResources value) {
        return new JAXBElement<EnlistTwoXAResources>(_EnlistTwoXAResources_QNAME, EnlistTwoXAResources.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatus }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "getStatus")
    public JAXBElement<GetStatus> createGetStatus(GetStatus value) {
        return new JAXBElement<GetStatus>(_GetStatus_QNAME, GetStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Sleep }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.simpleserver.wsat.ws.ibm.com/", name = "sleep")
    public JAXBElement<Sleep> createSleep(Sleep value) {
        return new JAXBElement<Sleep>(_Sleep_QNAME, Sleep.class, null, value);
    }

}
