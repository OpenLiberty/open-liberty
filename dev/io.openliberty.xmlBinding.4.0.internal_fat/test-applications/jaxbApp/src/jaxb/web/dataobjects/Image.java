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

import jakarta.activation.DataHandler;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlMimeType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>uploadImage complex type
 *
 * <p>
 *
 * <pre>
 * &lt;complexType name="uploadImage">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="arg0" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;element name="arg1" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Image", propOrder = { "name",
                                       "imageHandler",
                                       "imageBytes"
})
public class Image {

    protected String name;

    @XmlAttachmentRef
    @XmlMimeType("application/octet-stream")
    protected DataHandler imageHandler;

    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlElement(name = "imageBytes")
    protected byte[] imageBytes;

    /**
     *
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *
     *
     * @return
     *         possible object is {@link DataHandler }
     *
     */
    public DataHandler getImageHandler() {
        return imageHandler;
    }

    /**
     *
     *
     * @param value
     *                  allowed object is {@link DataHandler }
     *
     */
    public void setImageHandler(DataHandler value) {
        this.imageHandler = value;
    }

    /**
     *
     *
     * @return
     *         possible object is {@link DataHandler }
     *
     */
    public byte[] getImageBytes() {
        return imageBytes;
    }

    /**
     *
     *
     * @param value
     *                  allowed object is {@link DataHandler }
     *
     */
    public void setImageBytes(byte[] value) {
        this.imageBytes = value;
    }

}
