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
package com.ibm.ws.jaxws.namespacecheck.wsdl.importedserviceschema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for CustomFaultType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CustomFaultType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd}CFaultCode"/&gt;
 *         &lt;element ref="{http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd}CFaultString"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomFaultType", propOrder = {
                                                 "cFaultCode",
                                                 "cFaultString"
})
public class CustomFaultType {

    @XmlElement(name = "CFaultCode", required = true)
    protected String cFaultCode;
    @XmlElement(name = "CFaultString", required = true)
    protected String cFaultString;

    /**
     * Gets the value of the cFaultCode property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getCFaultCode() {
        return cFaultCode;
    }

    /**
     * Sets the value of the cFaultCode property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setCFaultCode(String value) {
        this.cFaultCode = value;
    }

    /**
     * Gets the value of the cFaultString property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getCFaultString() {
        return cFaultString;
    }

    /**
     * Sets the value of the cFaultString property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setCFaultString(String value) {
        this.cFaultString = value;
    }

}
