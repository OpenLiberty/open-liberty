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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>Java class for PurchaseOrderType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PurchaseOrderType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="shipTo" type="{}ShippingAddress"/>
 *         &lt;element name="billTo" type="{}ShippingAddress"/>
 *         &lt;element ref="{}comment" minOccurs="0"/>
 *         &lt;element name="items" type="{}Items"/>
 *       &lt;/sequence>
 *       &lt;attribute name="orderDate" type="{http://www.w3.org/2001/XMLSchema}date" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PurchaseOrderType", propOrder = {
                                                   "shipTo",
                                                   "billTo",
                                                   "comment",
                                                   "items"
})
public class PurchaseOrderType {

    @XmlElement(required = true)
    protected ShippingAddress shipTo;
    @XmlElement(required = true)
    protected ShippingAddress billTo;
    protected String comment;
    @XmlElement(required = true)
    protected Items items;
    @XmlAttribute(name = "orderDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar orderDate;

    /**
     * Gets the value of the shipTo property.
     *
     * @return
     *         possible object is
     *         {@link ShippingAddress }
     * 
     */
    public ShippingAddress getShipTo() {
        return shipTo;
    }

    /**
     * Sets the value of the shipTo property.
     *
     * @param value
     *                  allowed object is
     *                  {@link ShippingAddress }
     * 
     */
    public void setShipTo(ShippingAddress value) {
        this.shipTo = value;
    }

    /**
     * Gets the value of the billTo property.
     *
     * @return
     *         possible object is
     *         {@link ShippingAddress }
     * 
     */
    public ShippingAddress getBillTo() {
        return billTo;
    }

    /**
     * Sets the value of the billTo property.
     *
     * @param value
     *                  allowed object is
     *                  {@link ShippingAddress }
     * 
     */
    public void setBillTo(ShippingAddress value) {
        this.billTo = value;
    }

    /**
     * Gets the value of the comment property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setComment(String value) {
        this.comment = value;
    }

    /**
     * Gets the value of the items property.
     *
     * @return
     *         possible object is
     *         {@link Items }
     * 
     */
    public Items getItems() {
        return items;
    }

    /**
     * Sets the value of the items property.
     *
     * @param value
     *                  allowed object is
     *                  {@link Items }
     * 
     */
    public void setItems(Items value) {
        this.items = value;
    }

    /**
     * Gets the value of the orderDate property.
     *
     * @return
     *         possible object is
     *         {@link XMLGregorianCalendar }
     * 
     */
    public XMLGregorianCalendar getOrderDate() {
        return orderDate;
    }

    /**
     * Sets the value of the orderDate property.
     *
     * @param value
     *                  allowed object is
     *                  {@link XMLGregorianCalendar }
     * 
     */
    public void setOrderDate(XMLGregorianCalendar value) {
        this.orderDate = value;
    }

}
