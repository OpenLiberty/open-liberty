/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

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
