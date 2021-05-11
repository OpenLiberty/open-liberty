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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

<<<<<<< HEAD
/**
 * <p>Java class for sayHelloResponse complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
=======

/**
 * <p>Java class for sayHelloResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
>>>>>>> f8f25ee2d3... SOAPAction and test fixes
 * <pre>
 * &lt;complexType name="sayHelloResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="return" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
@XmlType(name = "sayHelloResponse", propOrder = {
                                                  "_return"
=======
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHelloResponse", propOrder = {
    "_return"
>>>>>>> f8f25ee2d3... SOAPAction and test fixes
})
public class SayHelloResponse {

    @XmlElement(name = "return")
    protected String _return;

    /**
     * Gets the value of the return property.
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
    public String getReturn() {
        return _return;
    }

    /**
     * Sets the value of the return property.
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
    public void setReturn(String value) {
        this._return = value;
    }

}
