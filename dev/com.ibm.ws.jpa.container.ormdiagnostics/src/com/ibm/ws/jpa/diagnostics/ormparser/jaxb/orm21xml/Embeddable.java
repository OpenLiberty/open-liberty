/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
// Generated on: 2017.04.12 at 04:14:13 PM CDT
//

package com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm21xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEmbeddable;

/**
 *
 *
 * Defines the settings and mappings for embeddable objects. Is
 * allowed to be sparsely populated and used in conjunction with
 * the annotations. Alternatively, the metadata-complete attribute
 * can be used to indicate that no annotations are to be processed
 * in the class. If this is the case then the defaulting rules will
 * be recursively applied.
 *
 * @Target({TYPE}) @Retention(RUNTIME)
 *                 public @interface Embeddable {}
 *
 * 
 *
 *                 <p>Java class for embeddable complex type.
 *
 *                 <p>The following schema fragment specifies the expected content contained within this class.
 *
 *                 <pre>
 * &lt;complexType name="embeddable">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="attributes" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}embeddable-attributes" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="access" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}access-type" />
 *       &lt;attribute name="metadata-complete" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 *                 </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "embeddable", propOrder = {
                                            "description",
                                            "attributes"
})
public class Embeddable implements IEmbeddable {

    protected String description;
    protected EmbeddableAttributes attributes;
    @XmlAttribute(name = "class", required = true)
    protected String clazz;
    @XmlAttribute(name = "access")
    protected AccessType access;
    @XmlAttribute(name = "metadata-complete")
    protected Boolean metadataComplete;

    /**
     * Gets the value of the description property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the attributes property.
     *
     * @return
     *         possible object is
     *         {@link EmbeddableAttributes }
     * 
     */
    public EmbeddableAttributes getAttributes() {
        return attributes;
    }

    /**
     * Sets the value of the attributes property.
     *
     * @param value
     *            allowed object is
     *            {@link EmbeddableAttributes }
     * 
     */
    public void setAttributes(EmbeddableAttributes value) {
        this.attributes = value;
    }

    /**
     * Gets the value of the clazz property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setClazz(String value) {
        this.clazz = value;
    }

    /**
     * Gets the value of the access property.
     *
     * @return
     *         possible object is
     *         {@link AccessType }
     * 
     */
    public AccessType getAccess() {
        return access;
    }

    /**
     * Sets the value of the access property.
     *
     * @param value
     *            allowed object is
     *            {@link AccessType }
     * 
     */
    public void setAccess(AccessType value) {
        this.access = value;
    }

    /**
     * Gets the value of the metadataComplete property.
     *
     * @return
     *         possible object is
     *         {@link Boolean }
     * 
     */
    public Boolean isMetadataComplete() {
        return metadataComplete;
    }

    /**
     * Sets the value of the metadataComplete property.
     *
     * @param value
     *            allowed object is
     *            {@link Boolean }
     * 
     */
    public void setMetadataComplete(Boolean value) {
        this.metadataComplete = value;
    }

}
