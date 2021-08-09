/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxws30.handler.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * This type is a general type that can be used to declare parameter/value lists.
 * <p>
 * Java class for param-valueType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="param-valueType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://java.sun.com/xml/ns/javaee}descriptionType"
 *                     maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="param-name" type="{http://java.sun.com/xml/ns/javaee}string"/>
 *         &lt;element name="param-value" type="{http://java.sun.com/xml/ns/javaee}xsdStringType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "param-valueType", propOrder = {
        "description", "paramName", "paramValue" })
public class ParamValueType {

    protected List<DescriptionType> description;
    @XmlElement(name = "param-name", required = true)
    protected CString paramName;
    @XmlElement(name = "param-value", required = true)
    protected XsdStringType paramValue;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected java.lang.String id;

    /**
     * Gets the value of the description property.
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the description property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getDescription().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link DescriptionType }
     */
    public List<DescriptionType> getDescription() {
        if (description == null) {
            description = new ArrayList<>();
        }
        return this.description;
    }

    /**
     * Gets the value of the paramName property.
     *
     * @return possible object is {@link CString }
     */
    public CString getParamName() {
        return paramName;
    }

    /**
     * Sets the value of the paramName property.
     *
     * @param value allowed object is {@link CString }
     */
    public void setParamName(CString value) {
        this.paramName = value;
    }

    /**
     * Gets the value of the paramValue property.
     *
     * @return possible object is {@link XsdStringType }
     */
    public XsdStringType getParamValue() {
        return paramValue;
    }

    /**
     * Sets the value of the paramValue property.
     *
     * @param value allowed object is {@link XsdStringType }
     */
    public void setParamValue(XsdStringType value) {
        this.paramValue = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link java.lang.String }
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link java.lang.String }
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }

}
