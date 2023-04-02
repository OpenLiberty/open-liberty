
/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxb.web.dataobjects;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    private final static QName _Image_QName = new QName("http://jaxb.web.dataobjects/", "Image");
    private final static QName _Items_QName = new QName("http://jaxb.web.dataobjects/", "Items");
    private final static QName _PurchaseOrderType_QName = new QName("http://jaxb.web.dataobjects/", "PurchaseOrderType");
    private final static QName _ShippingAddress_QName = new QName("http://jaxb.web.dataobjects/", "ShippingAddress");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: mtomservice
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetAttachment }
     *
     */

    public Image createImage() {
        return new Image();
    }

    /**
     * Create an instance of {@link SendAttachment }
     *
     */
    public Items createItems() {
        return new Items();
    }

    /**
     * Create an instance of {@link GetAttachmentResponse }
     *
     */
    public PurchaseOrderType createPurchaseOrderType() {
        return new PurchaseOrderType();
    }

    /**
     * Create an instance of {@link SendAttachmentResponse }
     *
     */
    public ShippingAddress createShippingAddress() {
        return new ShippingAddress();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAttachmentResponse }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://jaxb.web.dataobjects/", name = "Image")
    public JAXBElement<Image> createGetAttachmentResponse(Image value) {
        return new JAXBElement<Image>(_Image_QName, Image.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendAttachment }{@code >}}
     * private final static QName _ShippingAddress_QName = new QName("http://jaxb.web.dataobjects/", "ShippingAddress");
     *
     */
    @XmlElementDecl(namespace = "http://jaxb.web.dataobjects/", name = "Items")
    public JAXBElement<Items> createItems(Items value) {
        return new JAXBElement<Items>(_Items_QName, Items.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendAttachmentResponse }{@code >}}
     *
     * private final static QName _PurchaseOrderType_QName = new QName("http://jaxb.web.dataobjects/", "PurchaseOrderType");
     *
     */
    @XmlElementDecl(namespace = "http://jaxb.web.dataobjects/", name = "PurchaseOrderType")
    public JAXBElement<PurchaseOrderType> createPurchaseOrderType(PurchaseOrderType value) {
        return new JAXBElement<PurchaseOrderType>(_PurchaseOrderType_QName, PurchaseOrderType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAttachment }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://jaxb.web.dataobjects/", name = "ShippingAddress")
    public JAXBElement<ShippingAddress> createShippingAddress(ShippingAddress value) {
        return new JAXBElement<ShippingAddress>(_ShippingAddress_QName, ShippingAddress.class, null, value);
    }

}
