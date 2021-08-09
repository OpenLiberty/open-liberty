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
package com.ibm.samples.jaxws.spring.wsdlfirst.stub;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.w3._2001.xmlschema.Adapter2;

/**
 * <p>Java class for customer complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="customer">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="customerId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 * &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;element name="address" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="numOrders" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 * &lt;element name="revenue" type="{http://www.w3.org/2001/XMLSchema}double"/>
 * &lt;element name="test" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/>
 * &lt;element name="birthDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 * &lt;element name="type" type="{http://customerservice.example.com/}customerType" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "customer", propOrder = {
                                          "customerId",
                                          "name",
                                          "address",
                                          "numOrders",
                                          "revenue",
                                          "test",
                                          "birthDate",
                                          "type"
})
public class Customer {

    protected int customerId;
    protected String name;
    @XmlElement(nillable = true)
    protected List<String> address;
    protected Integer numOrders;
    protected double revenue;
    protected BigDecimal test;
    @XmlElement(type = String.class)
    @XmlJavaTypeAdapter(Adapter2.class)
    @XmlSchemaType(name = "date")
    protected Date birthDate;
    protected CustomerType type;

    /**
     * Gets the value of the customerId property.
     *
     */
    public int getCustomerId() {
        return customerId;
    }

    /**
     * Sets the value of the customerId property.
     *
     */
    public void setCustomerId(int value) {
        this.customerId = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the address property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the address property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getAddress().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     *
     */
    public List<String> getAddress() {
        if (address == null) {
            address = new ArrayList<String>();
        }
        return this.address;
    }

    /**
     * Gets the value of the numOrders property.
     *
     * @return
     *         possible object is {@link Integer }
     *
     */
    public Integer getNumOrders() {
        return numOrders;
    }

    /**
     * Sets the value of the numOrders property.
     *
     * @param value
     *                  allowed object is {@link Integer }
     *
     */
    public void setNumOrders(Integer value) {
        this.numOrders = value;
    }

    /**
     * Gets the value of the revenue property.
     *
     */
    public double getRevenue() {
        return revenue;
    }

    /**
     * Sets the value of the revenue property.
     *
     */
    public void setRevenue(double value) {
        this.revenue = value;
    }

    /**
     * Gets the value of the test property.
     *
     * @return
     *         possible object is {@link BigDecimal }
     *
     */
    public BigDecimal getTest() {
        return test;
    }

    /**
     * Sets the value of the test property.
     *
     * @param value
     *                  allowed object is {@link BigDecimal }
     *
     */
    public void setTest(BigDecimal value) {
        this.test = value;
    }

    /**
     * Gets the value of the birthDate property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public Date getBirthDate() {
        return birthDate;
    }

    /**
     * Sets the value of the birthDate property.
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setBirthDate(Date value) {
        this.birthDate = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return
     *         possible object is {@link CustomerType }
     *
     */
    public CustomerType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value
     *                  allowed object is {@link CustomerType }
     *
     */
    public void setType(CustomerType value) {
        this.type = value;
    }

}
