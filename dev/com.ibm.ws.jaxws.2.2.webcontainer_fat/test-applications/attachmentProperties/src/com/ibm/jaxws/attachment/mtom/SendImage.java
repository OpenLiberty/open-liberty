/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.jaxws.attachment.mtom;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="data" type="{http://com/ibm/jaxws/attachment/mtom/}ImageDepot" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
                                  "data"
})
@XmlRootElement(name = "sendImage")
public class SendImage {

    protected ImageDepot data;

    /**
     * Gets the value of the data property.
     *
     * @return
     *         possible object is
     *         {@link ImageDepot }
     *
     */
    public ImageDepot getData() {
        return data;
    }

    /**
     * Sets the value of the data property.
     *
     * @param value
     *                  allowed object is
     *                  {@link ImageDepot }
     *
     */
    public void setData(ImageDepot value) {
        this.data = value;
    }

}
