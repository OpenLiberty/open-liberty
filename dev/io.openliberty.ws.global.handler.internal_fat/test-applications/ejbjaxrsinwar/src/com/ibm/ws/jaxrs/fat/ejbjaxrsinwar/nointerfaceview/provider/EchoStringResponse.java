/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider;

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
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="echoStringResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
                                 "echoStringResult"
})
@XmlRootElement(name = "echoStringResponse")
public class EchoStringResponse {

    protected String echoStringResult;

    /**
     * Gets the value of the echoStringResult property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getEchoStringResult() {
        return echoStringResult;
    }

    /**
     * Sets the value of the echoStringResult property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setEchoStringResult(String value) {
        this.echoStringResult = value;
    }

}
