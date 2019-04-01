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
package mtomservice;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the mtomservice package.
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

    private final static QName _GetAttachmentResponse_QNAME = new QName("http://MTOMService/", "getAttachmentResponse");
    private final static QName _SendAttachment_QNAME = new QName("http://MTOMService/", "sendAttachment");
    private final static QName _SendAttachmentResponse_QNAME = new QName("http://MTOMService/", "sendAttachmentResponse");
    private final static QName _GetAttachment_QNAME = new QName("http://MTOMService/", "getAttachment");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: mtomservice
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link GetAttachment }
     *
     */
    public GetAttachment createGetAttachment() {
        return new GetAttachment();
    }

    /**
     * Create an instance of {@link SendAttachment }
     *
     */
    public SendAttachment createSendAttachment() {
        return new SendAttachment();
    }

    /**
     * Create an instance of {@link GetAttachmentResponse }
     *
     */
    public GetAttachmentResponse createGetAttachmentResponse() {
        return new GetAttachmentResponse();
    }

    /**
     * Create an instance of {@link SendAttachmentResponse }
     *
     */
    public SendAttachmentResponse createSendAttachmentResponse() {
        return new SendAttachmentResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAttachmentResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://MTOMService/", name = "getAttachmentResponse")
    public JAXBElement<GetAttachmentResponse> createGetAttachmentResponse(GetAttachmentResponse value) {
        return new JAXBElement<GetAttachmentResponse>(_GetAttachmentResponse_QNAME, GetAttachmentResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendAttachment }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://MTOMService/", name = "sendAttachment")
    public JAXBElement<SendAttachment> createSendAttachment(SendAttachment value) {
        return new JAXBElement<SendAttachment>(_SendAttachment_QNAME, SendAttachment.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendAttachmentResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://MTOMService/", name = "sendAttachmentResponse")
    public JAXBElement<SendAttachmentResponse> createSendAttachmentResponse(SendAttachmentResponse value) {
        return new JAXBElement<SendAttachmentResponse>(_SendAttachmentResponse_QNAME, SendAttachmentResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAttachment }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://MTOMService/", name = "getAttachment")
    public JAXBElement<GetAttachment> createGetAttachment(GetAttachment value) {
        return new JAXBElement<GetAttachment>(_GetAttachment_QNAME, GetAttachment.class, null, value);
    }

}
