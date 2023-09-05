/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.properties.test.client.stub;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

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
@XmlType(name = "uploadImage", propOrder = {
                                             "arg0",
                                             "arg1"
})
public class UploadImage {

    protected String arg0;

    @XmlAttachmentRef
    @XmlMimeType("application/octet-stream")
    protected DataHandler arg1;

    /**
     *
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getArg0() {
        return arg0;
    }

    /**
     *
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setArg0(String value) {
        this.arg0 = value;
    }

    /**
     *
     *
     * @return
     *         possible object is {@link DataHandler }
     *
     */
    public DataHandler getArg1() {
        return arg1;
    }

    /**
     *
     *
     * @param value
     *                  allowed object is {@link DataHandler }
     *
     */
    public void setArg1(DataHandler value) {
        this.arg1 = value;
    }

}
