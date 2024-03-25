/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http:www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi.beanvalidation.stubs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for twoWayWithValidationJAXBAnnotatedMethodValidation complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="twoWayWithValidationJAXBAnnotatedMethodValidation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="arg0" type="{http://beanvalidation.jaxws.ws.ibm.com}twoWayWithValidation1" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "twoWayWithValidationJAXBAnnotatedMethodValidation1", propOrder = {
                                                                                    "arg0"
})
public class TwoWayWithValidationJAXBAnnotatedMethodValidation {

    protected TwoWayWithValidation arg0;

    /**
     * Gets the value of the arg0 property.
     *
     * @return
     *         possible object is
     *         {@link TwoWayWithValidation1 }
     *
     */
    public TwoWayWithValidation getArg0() {
        return arg0;
    }

    /**
     * Sets the value of the arg0 property.
     *
     * @param value
     *                  allowed object is
     *                  {@link TwoWayWithValidation1 }
     *
     */
    public void setArg0(TwoWayWithValidation value) {
        this.arg0 = value;
    }

}
