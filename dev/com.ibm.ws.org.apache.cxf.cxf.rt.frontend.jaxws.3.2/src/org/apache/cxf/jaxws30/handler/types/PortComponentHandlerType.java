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
 * Declares the handler for a port-component. Handlers can access the init-param name/value pairs using the
 * HandlerInfo interface. Used in: port-component
 * <p>
 * Java class for port-component_handlerType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="port-component_handlerType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://java.sun.com/xml/ns/javaee}descriptionGroup"/>
 *         &lt;element name="handler-name" type="{http://java.sun.com/xml/ns/javaee}string"/>
 *         &lt;element name="handler-class" type="{http://java.sun.com/xml/ns/javaee}fully-qualified-classType"/>
 *         &lt;element name="init-param" type="{http://java.sun.com/xml/ns/javaee}param-valueType"
 *                     maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="soap-header" type="{http://java.sun.com/xml/ns/javaee}xsdQNameType"
 *                     maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="soap-role" type="{http://java.sun.com/xml/ns/javaee}string"
 *                     maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "port-component_handlerType", propOrder = {
        "description", "displayName", "icon", "handlerName", "handlerClass", "initParam", "soapHeader",
        "soapRole" })
public class PortComponentHandlerType {

    protected List<DescriptionType> description;
    @XmlElement(name = "display-name")
    protected List<DisplayNameType> displayName;
    protected List<IconType> icon;
    @XmlElement(name = "handler-name", required = true)
    protected CString handlerName;
    @XmlElement(name = "handler-class", required = true)
    protected FullyQualifiedClassType handlerClass;
    @XmlElement(name = "init-param")
    protected List<ParamValueType> initParam;
    @XmlElement(name = "soap-header")
    protected List<XsdQNameType> soapHeader;
    @XmlElement(name = "soap-role")
    protected List<CString> soapRole;
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
     * Gets the value of the displayName property.
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the displayName property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getDisplayName().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link DisplayNameType }
     */
    public List<DisplayNameType> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<>();
        }
        return this.displayName;
    }

    /**
     * Gets the value of the icon property.
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the icon property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getIcon().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link IconType }
     */
    public List<IconType> getIcon() {
        if (icon == null) {
            icon = new ArrayList<>();
        }
        return this.icon;
    }

    /**
     * Gets the value of the handlerName property.
     *
     * @return possible object is {@link CString }
     */
    public CString getHandlerName() {
        if (handlerName == null) {
            handlerName = new CString();
            handlerName.setValue("");
        }
        return handlerName;
    }

    /**
     * Sets the value of the handlerName property.
     *
     * @param value allowed object is {@link CString }
     */
    public void setHandlerName(CString value) {
        this.handlerName = value;
    }

    /**
     * Gets the value of the handlerClass property.
     *
     * @return possible object is {@link FullyQualifiedClassType }
     */
    public FullyQualifiedClassType getHandlerClass() {
        return handlerClass;
    }

    /**
     * Sets the value of the handlerClass property.
     *
     * @param value allowed object is {@link FullyQualifiedClassType }
     */
    public void setHandlerClass(FullyQualifiedClassType value) {
        this.handlerClass = value;
    }

    /**
     * Gets the value of the initParam property.
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the initParam property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getInitParam().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link ParamValueType }
     */
    public List<ParamValueType> getInitParam() {
        if (initParam == null) {
            initParam = new ArrayList<>();
        }
        return this.initParam;
    }

    /**
     * Gets the value of the soapHeader property.
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the soapHeader property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSoapHeader().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link XsdQNameType }
     */
    public List<XsdQNameType> getSoapHeader() {
        if (soapHeader == null) {
            soapHeader = new ArrayList<>();
        }
        return this.soapHeader;
    }

    /**
     * Gets the value of the soapRole property.
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification
     * you make to the returned list will be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the soapRole property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSoapRole().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link CString }
     */
    public List<CString> getSoapRole() {
        if (soapRole == null) {
            soapRole = new ArrayList<>();
        }
        return this.soapRole;
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
