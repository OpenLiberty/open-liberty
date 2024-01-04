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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for NameSpaceCheckMessage complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="NameSpaceCheckMessage"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://com/ibm/ws/jaxws/wsdl/MainServiceSchema.xsd}Dummy_Object"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NameSpaceCheckMessage", propOrder = {
                                                       "dummyObject"
})
public class NameSpaceCheckMessage {

    @XmlElement(name = "Dummy_Object", required = true)
    protected DummyObjectTYPE dummyObject;

    /**
     * Gets the value of the dummyObject property.
     *
     * @return
     *         possible object is
     *         {@link DummyObjectTYPE }
     * 
     */
    public DummyObjectTYPE getDummyObject() {
        return dummyObject;
    }

    /**
     * Sets the value of the dummyObject property.
     *
     * @param value
     *                  allowed object is
     *                  {@link DummyObjectTYPE }
     * 
     */
    public void setDummyObject(DummyObjectTYPE value) {
        this.dummyObject = value;
    }

}
