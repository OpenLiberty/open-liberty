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

import java.util.List;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for ImageDepot complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ImageDepot">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="imageData" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageDepot", namespace = "http://com/ibm/jaxws/attachment/mtom/", propOrder = {
                                                                                                 "imageDataList"
})
public class ImageDepot {

    @XmlElement(required = true)
    @XmlMimeType("multipart/*")
    protected List<DataHandler> imageDataList;

    /**
     * Gets the value of the imageData property.
     *
     * @return
     *         possible object is
     *         {@link DataHandler }
     *
     */
    public List<DataHandler> getImageData() {
        return imageDataList;
    }

    /**
     * Sets the value of the imageData property.
     *
     * @param value
     *                  allowed object is
     *                  {@link DataHandler }
     *
     */
    public void setImageData(List<DataHandler> value) {
        this.imageDataList = value;
    }

}
