<<<<<<< HEAD
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
=======

>>>>>>> f8f25ee2d3... SOAPAction and test fixes
package com.ibm.samples.jaxws2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

<<<<<<< HEAD
/**
 * <p>Java class for sayHello complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
=======

/**
 * <p>Java class for sayHello complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
>>>>>>> f8f25ee2d3... SOAPAction and test fixes
 * <pre>
 * &lt;complexType name="sayHello">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="arg0" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
<<<<<<< HEAD
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHello", propOrder = {
                                          "arg0"
=======
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHello", propOrder = {
    "arg0"
>>>>>>> f8f25ee2d3... SOAPAction and test fixes
})
public class SayHello_Type {

    protected String arg0;

    /**
     * Gets the value of the arg0 property.
<<<<<<< HEAD
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
=======
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
>>>>>>> f8f25ee2d3... SOAPAction and test fixes
     */
    public String getArg0() {
        return arg0;
    }

    /**
     * Sets the value of the arg0 property.
<<<<<<< HEAD
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
=======
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
>>>>>>> f8f25ee2d3... SOAPAction and test fixes
     */
    public void setArg0(String value) {
        this.arg0 = value;
    }

}
