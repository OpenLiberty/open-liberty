/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for Dummy_Object_TYPE complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Dummy_Object_TYPE"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd}ID"/&gt;
 *         &lt;element ref="{http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd}DESCRIPTION"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Dummy_Object_TYPE", propOrder = {
                                                   "id",
                                                   "description"
})
public class DummyObjectTYPE {

    @XmlElement(name = "ID", required = true, nillable = true)
    protected BigInteger id;
    @XmlElement(name = "DESCRIPTION", required = true)
    protected String description;

    /**
     * Gets the value of the id property.
     *
     * @return
     *         possible object is
     *         {@link BigInteger }
     * 
     */
    public BigInteger getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *                  allowed object is
     *                  {@link BigInteger }
     * 
     */
    public void setID(BigInteger value) {
        this.id = value;
    }

    /**
     * Gets the value of the description property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getDESCRIPTION() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setDESCRIPTION(String value) {
        this.description = value;
    }

}
