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
package fats.cxf.jaxws22.mtom.server;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for echobyte64 complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="echobyte64">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="arg0" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "echobyte64", propOrder = {
                                            "arg0"
})
public class Echobyte64 {

    @XmlElementRef(name = "arg0", type = JAXBElement.class, required = false)
    protected JAXBElement<byte[]> arg0;

    /**
     * Gets the value of the arg0 property.
     *
     * @return
     *         possible object is
     *         {@link JAXBElement }{@code <}{@link byte[]}{@code >}
     * 
     */
    public JAXBElement<byte[]> getArg0() {
        return arg0;
    }

    /**
     * Sets the value of the arg0 property.
     *
     * @param value
     *            allowed object is
     *            {@link JAXBElement }{@code <}{@link byte[]}{@code >}
     * 
     */
    public void setArg0(JAXBElement<byte[]> value) {
        this.arg0 = (value);
    }

}
