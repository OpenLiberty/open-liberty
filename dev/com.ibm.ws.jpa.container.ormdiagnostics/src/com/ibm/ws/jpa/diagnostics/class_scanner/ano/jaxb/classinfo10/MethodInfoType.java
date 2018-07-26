/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.02.08 at 03:24:59 PM CST 
//


package com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MethodInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MethodInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="modifiers" type="{ClassInfo_1.0}ModifiersType" minOccurs="0"/>
 *         &lt;element name="parameters" type="{ClassInfo_1.0}ParametersType" minOccurs="0"/>
 *         &lt;element name="annotations" type="{ClassInfo_1.0}AnnotationsType" minOccurs="0"/>
 *         &lt;element name="exceptions" type="{ClassInfo_1.0}ExceptionsType" minOccurs="0"/>
 *         &lt;element name="properties" type="{ClassInfo_1.0}PropertiesType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="methodName" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="returnType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="isCtor" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MethodInfoType", propOrder = {
    "modifiers",
    "parameters",
    "annotations",
    "exceptions",
    "properties"
})
public class MethodInfoType {

    protected ModifiersType modifiers;
    protected ParametersType parameters;
    protected AnnotationsType annotations;
    protected ExceptionsType exceptions;
    protected PropertiesType properties;
    @XmlAttribute(name = "methodName", required = true)
    protected String methodName;
    @XmlAttribute(name = "returnType", required = true)
    protected String returnType;
    @XmlAttribute(name = "isCtor")
    protected Boolean isCtor;

    /**
     * Gets the value of the modifiers property.
     * 
     * @return
     *     possible object is
     *     {@link ModifiersType }
     *     
     */
    public ModifiersType getModifiers() {
        return modifiers;
    }

    /**
     * Sets the value of the modifiers property.
     * 
     * @param value
     *     allowed object is
     *     {@link ModifiersType }
     *     
     */
    public void setModifiers(ModifiersType value) {
        this.modifiers = value;
    }

    /**
     * Gets the value of the parameters property.
     * 
     * @return
     *     possible object is
     *     {@link ParametersType }
     *     
     */
    public ParametersType getParameters() {
        return parameters;
    }

    /**
     * Sets the value of the parameters property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParametersType }
     *     
     */
    public void setParameters(ParametersType value) {
        this.parameters = value;
    }

    /**
     * Gets the value of the annotations property.
     * 
     * @return
     *     possible object is
     *     {@link AnnotationsType }
     *     
     */
    public AnnotationsType getAnnotations() {
        return annotations;
    }

    /**
     * Sets the value of the annotations property.
     * 
     * @param value
     *     allowed object is
     *     {@link AnnotationsType }
     *     
     */
    public void setAnnotations(AnnotationsType value) {
        this.annotations = value;
    }

    /**
     * Gets the value of the exceptions property.
     * 
     * @return
     *     possible object is
     *     {@link ExceptionsType }
     *     
     */
    public ExceptionsType getExceptions() {
        return exceptions;
    }

    /**
     * Sets the value of the exceptions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExceptionsType }
     *     
     */
    public void setExceptions(ExceptionsType value) {
        this.exceptions = value;
    }

    /**
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesType }
     *     
     */
    public PropertiesType getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesType }
     *     
     */
    public void setProperties(PropertiesType value) {
        this.properties = value;
    }

    /**
     * Gets the value of the methodName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the value of the methodName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMethodName(String value) {
        this.methodName = value;
    }

    /**
     * Gets the value of the returnType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * Sets the value of the returnType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReturnType(String value) {
        this.returnType = value;
    }

    /**
     * Gets the value of the isCtor property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsCtor() {
        return isCtor;
    }

    /**
     * Sets the value of the isCtor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsCtor(Boolean value) {
        this.isCtor = value;
    }

}
