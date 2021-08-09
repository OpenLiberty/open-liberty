/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.webservice.client.wscoor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.w3c.dom.Element;

/**
 * <p>Java class for RegisterResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RegisterResponseType">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="CoordinatorProtocolService" type="{http://schemas.xmlsoap.org/ws/2004/08/addressing}EndpointReferenceType"/>
 * &lt;any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;anyAttribute processContents='lax' namespace='##other'/>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RegisterResponseType", propOrder = {
                                                     "coordinatorProtocolService",
                                                     "any"
})
public class RegisterResponseType {

    @XmlElement(name = "CoordinatorProtocolService", required = true)
    protected EndpointReferenceType coordinatorProtocolService;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAnyAttribute
    private final Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the coordinatorProtocolService property.
     * 
     * @return
     *         possible object is {@link EndpointReferenceType }
     * 
     */
    public EndpointReferenceType getCoordinatorProtocolService() {
        return coordinatorProtocolService;
    }

    /**
     * Sets the value of the coordinatorProtocolService property.
     * 
     * @param value
     *            allowed object is {@link EndpointReferenceType }
     * 
     */
    public void setCoordinatorProtocolService(EndpointReferenceType value) {
        this.coordinatorProtocolService = value;
    }

    /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Object } {@link Element }
     * 
     * 
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *         always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
