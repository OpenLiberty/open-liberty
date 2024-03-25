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
 * <p>Java class for CustomHeaderType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CustomHeaderType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://com/ibm/ws/jaxws/wsdl/ImportedServiceSchema.xsd}CPerson"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomHeaderType", propOrder = {
                                                  "cPerson"
})
public class CustomHeaderType {

    @XmlElement(name = "CPerson", required = true)
    protected PersonType cPerson;

    /**
     * Gets the value of the cPerson property.
     *
     * @return
     *         possible object is
     *         {@link PersonType }
     * 
     */
    public PersonType getCPerson() {
        return cPerson;
    }

    /**
     * Sets the value of the cPerson property.
     *
     * @param value
     *                  allowed object is
     *                  {@link PersonType }
     * 
     */
    public void setCPerson(PersonType value) {
        this.cPerson = value;
    }

}
