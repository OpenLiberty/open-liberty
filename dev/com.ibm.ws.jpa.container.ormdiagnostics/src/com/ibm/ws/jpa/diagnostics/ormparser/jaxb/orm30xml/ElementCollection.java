/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
// Generated on: 2018.06.07 at 01:36:00 PM CDT 
//


package com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm30xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 
 *         @Target({METHOD, FIELD}) @Retention(RUNTIME)
 *         public @interface ElementCollection {
 *           Class targetClass() default void.class;
 *           FetchType fetch() default LAZY;
 *         }
 * 
 *       
 * 
 * <p>Java class for element-collection complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="element-collection">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="order-by" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}order-by" minOccurs="0"/>
 *           &lt;element name="order-column" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}order-column" minOccurs="0"/>
 *         &lt;/choice>
 *         &lt;choice>
 *           &lt;element name="map-key" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}map-key" minOccurs="0"/>
 *           &lt;sequence>
 *             &lt;element name="map-key-class" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}map-key-class" minOccurs="0"/>
 *             &lt;choice>
 *               &lt;element name="map-key-temporal" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}temporal" minOccurs="0"/>
 *               &lt;element name="map-key-enumerated" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}enumerated" minOccurs="0"/>
 *               &lt;sequence>
 *                 &lt;element name="map-key-attribute-override" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}attribute-override" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="map-key-convert" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}convert" maxOccurs="unbounded" minOccurs="0"/>
 *               &lt;/sequence>
 *             &lt;/choice>
 *             &lt;choice>
 *               &lt;element name="map-key-column" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}map-key-column" minOccurs="0"/>
 *               &lt;sequence>
 *                 &lt;element name="map-key-join-column" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}map-key-join-column" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="map-key-foreign-key" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}foreign-key" minOccurs="0"/>
 *               &lt;/sequence>
 *             &lt;/choice>
 *           &lt;/sequence>
 *         &lt;/choice>
 *         &lt;choice>
 *           &lt;sequence>
 *             &lt;element name="column" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}column" minOccurs="0"/>
 *             &lt;choice>
 *               &lt;element name="temporal" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}temporal" minOccurs="0"/>
 *               &lt;element name="enumerated" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}enumerated" minOccurs="0"/>
 *               &lt;element name="lob" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}lob" minOccurs="0"/>
 *             &lt;/choice>
 *           &lt;/sequence>
 *           &lt;sequence>
 *             &lt;element name="attribute-override" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}attribute-override" maxOccurs="unbounded" minOccurs="0"/>
 *             &lt;element name="association-override" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}association-override" maxOccurs="unbounded" minOccurs="0"/>
 *             &lt;element name="convert" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}convert" maxOccurs="unbounded" minOccurs="0"/>
 *           &lt;/sequence>
 *         &lt;/choice>
 *         &lt;element name="collection-table" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}collection-table" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="target-class" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fetch" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}fetch-type" />
 *       &lt;attribute name="access" type="{http://xmlns.jcp.org/xml/ns/persistence/orm}access-type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "element-collection", propOrder = {
    "orderBy",
    "orderColumn",
    "mapKey",
    "mapKeyClass",
    "mapKeyTemporal",
    "mapKeyEnumerated",
    "mapKeyAttributeOverride",
    "mapKeyConvert",
    "mapKeyColumn",
    "mapKeyJoinColumn",
    "mapKeyForeignKey",
    "column",
    "temporal",
    "enumerated",
    "lob",
    "attributeOverride",
    "associationOverride",
    "convert",
    "collectionTable"
})
public class ElementCollection {

    @XmlElement(name = "order-by")
    protected String orderBy;
    @XmlElement(name = "order-column")
    protected OrderColumn orderColumn;
    @XmlElement(name = "map-key")
    protected MapKey mapKey;
    @XmlElement(name = "map-key-class")
    protected MapKeyClass mapKeyClass;
    @XmlElement(name = "map-key-temporal")
    @XmlSchemaType(name = "token")
    protected TemporalType mapKeyTemporal;
    @XmlElement(name = "map-key-enumerated")
    @XmlSchemaType(name = "token")
    protected EnumType mapKeyEnumerated;
    @XmlElement(name = "map-key-attribute-override")
    protected List<AttributeOverride> mapKeyAttributeOverride;
    @XmlElement(name = "map-key-convert")
    protected List<Convert> mapKeyConvert;
    @XmlElement(name = "map-key-column")
    protected MapKeyColumn mapKeyColumn;
    @XmlElement(name = "map-key-join-column")
    protected List<MapKeyJoinColumn> mapKeyJoinColumn;
    @XmlElement(name = "map-key-foreign-key")
    protected ForeignKey mapKeyForeignKey;
    protected Column column;
    @XmlSchemaType(name = "token")
    protected TemporalType temporal;
    @XmlSchemaType(name = "token")
    protected EnumType enumerated;
    protected Lob lob;
    @XmlElement(name = "attribute-override")
    protected List<AttributeOverride> attributeOverride;
    @XmlElement(name = "association-override")
    protected List<AssociationOverride> associationOverride;
    protected List<Convert> convert;
    @XmlElement(name = "collection-table")
    protected CollectionTable collectionTable;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "target-class")
    protected String targetClass;
    @XmlAttribute(name = "fetch")
    protected FetchType fetch;
    @XmlAttribute(name = "access")
    protected AccessType access;

    /**
     * Gets the value of the orderBy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * Sets the value of the orderBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrderBy(String value) {
        this.orderBy = value;
    }

    /**
     * Gets the value of the orderColumn property.
     * 
     * @return
     *     possible object is
     *     {@link OrderColumn }
     *     
     */
    public OrderColumn getOrderColumn() {
        return orderColumn;
    }

    /**
     * Sets the value of the orderColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrderColumn }
     *     
     */
    public void setOrderColumn(OrderColumn value) {
        this.orderColumn = value;
    }

    /**
     * Gets the value of the mapKey property.
     * 
     * @return
     *     possible object is
     *     {@link MapKey }
     *     
     */
    public MapKey getMapKey() {
        return mapKey;
    }

    /**
     * Sets the value of the mapKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link MapKey }
     *     
     */
    public void setMapKey(MapKey value) {
        this.mapKey = value;
    }

    /**
     * Gets the value of the mapKeyClass property.
     * 
     * @return
     *     possible object is
     *     {@link MapKeyClass }
     *     
     */
    public MapKeyClass getMapKeyClass() {
        return mapKeyClass;
    }

    /**
     * Sets the value of the mapKeyClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link MapKeyClass }
     *     
     */
    public void setMapKeyClass(MapKeyClass value) {
        this.mapKeyClass = value;
    }

    /**
     * Gets the value of the mapKeyTemporal property.
     * 
     * @return
     *     possible object is
     *     {@link TemporalType }
     *     
     */
    public TemporalType getMapKeyTemporal() {
        return mapKeyTemporal;
    }

    /**
     * Sets the value of the mapKeyTemporal property.
     * 
     * @param value
     *     allowed object is
     *     {@link TemporalType }
     *     
     */
    public void setMapKeyTemporal(TemporalType value) {
        this.mapKeyTemporal = value;
    }

    /**
     * Gets the value of the mapKeyEnumerated property.
     * 
     * @return
     *     possible object is
     *     {@link EnumType }
     *     
     */
    public EnumType getMapKeyEnumerated() {
        return mapKeyEnumerated;
    }

    /**
     * Sets the value of the mapKeyEnumerated property.
     * 
     * @param value
     *     allowed object is
     *     {@link EnumType }
     *     
     */
    public void setMapKeyEnumerated(EnumType value) {
        this.mapKeyEnumerated = value;
    }

    /**
     * Gets the value of the mapKeyAttributeOverride property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mapKeyAttributeOverride property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMapKeyAttributeOverride().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AttributeOverride }
     * 
     * 
     */
    public List<AttributeOverride> getMapKeyAttributeOverride() {
        if (mapKeyAttributeOverride == null) {
            mapKeyAttributeOverride = new ArrayList<AttributeOverride>();
        }
        return this.mapKeyAttributeOverride;
    }

    /**
     * Gets the value of the mapKeyConvert property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mapKeyConvert property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMapKeyConvert().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Convert }
     * 
     * 
     */
    public List<Convert> getMapKeyConvert() {
        if (mapKeyConvert == null) {
            mapKeyConvert = new ArrayList<Convert>();
        }
        return this.mapKeyConvert;
    }

    /**
     * Gets the value of the mapKeyColumn property.
     * 
     * @return
     *     possible object is
     *     {@link MapKeyColumn }
     *     
     */
    public MapKeyColumn getMapKeyColumn() {
        return mapKeyColumn;
    }

    /**
     * Sets the value of the mapKeyColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link MapKeyColumn }
     *     
     */
    public void setMapKeyColumn(MapKeyColumn value) {
        this.mapKeyColumn = value;
    }

    /**
     * Gets the value of the mapKeyJoinColumn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mapKeyJoinColumn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMapKeyJoinColumn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MapKeyJoinColumn }
     * 
     * 
     */
    public List<MapKeyJoinColumn> getMapKeyJoinColumn() {
        if (mapKeyJoinColumn == null) {
            mapKeyJoinColumn = new ArrayList<MapKeyJoinColumn>();
        }
        return this.mapKeyJoinColumn;
    }

    /**
     * Gets the value of the mapKeyForeignKey property.
     * 
     * @return
     *     possible object is
     *     {@link ForeignKey }
     *     
     */
    public ForeignKey getMapKeyForeignKey() {
        return mapKeyForeignKey;
    }

    /**
     * Sets the value of the mapKeyForeignKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link ForeignKey }
     *     
     */
    public void setMapKeyForeignKey(ForeignKey value) {
        this.mapKeyForeignKey = value;
    }

    /**
     * Gets the value of the column property.
     * 
     * @return
     *     possible object is
     *     {@link Column }
     *     
     */
    public Column getColumn() {
        return column;
    }

    /**
     * Sets the value of the column property.
     * 
     * @param value
     *     allowed object is
     *     {@link Column }
     *     
     */
    public void setColumn(Column value) {
        this.column = value;
    }

    /**
     * Gets the value of the temporal property.
     * 
     * @return
     *     possible object is
     *     {@link TemporalType }
     *     
     */
    public TemporalType getTemporal() {
        return temporal;
    }

    /**
     * Sets the value of the temporal property.
     * 
     * @param value
     *     allowed object is
     *     {@link TemporalType }
     *     
     */
    public void setTemporal(TemporalType value) {
        this.temporal = value;
    }

    /**
     * Gets the value of the enumerated property.
     * 
     * @return
     *     possible object is
     *     {@link EnumType }
     *     
     */
    public EnumType getEnumerated() {
        return enumerated;
    }

    /**
     * Sets the value of the enumerated property.
     * 
     * @param value
     *     allowed object is
     *     {@link EnumType }
     *     
     */
    public void setEnumerated(EnumType value) {
        this.enumerated = value;
    }

    /**
     * Gets the value of the lob property.
     * 
     * @return
     *     possible object is
     *     {@link Lob }
     *     
     */
    public Lob getLob() {
        return lob;
    }

    /**
     * Sets the value of the lob property.
     * 
     * @param value
     *     allowed object is
     *     {@link Lob }
     *     
     */
    public void setLob(Lob value) {
        this.lob = value;
    }

    /**
     * Gets the value of the attributeOverride property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attributeOverride property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttributeOverride().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AttributeOverride }
     * 
     * 
     */
    public List<AttributeOverride> getAttributeOverride() {
        if (attributeOverride == null) {
            attributeOverride = new ArrayList<AttributeOverride>();
        }
        return this.attributeOverride;
    }

    /**
     * Gets the value of the associationOverride property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the associationOverride property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAssociationOverride().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AssociationOverride }
     * 
     * 
     */
    public List<AssociationOverride> getAssociationOverride() {
        if (associationOverride == null) {
            associationOverride = new ArrayList<AssociationOverride>();
        }
        return this.associationOverride;
    }

    /**
     * Gets the value of the convert property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the convert property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConvert().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Convert }
     * 
     * 
     */
    public List<Convert> getConvert() {
        if (convert == null) {
            convert = new ArrayList<Convert>();
        }
        return this.convert;
    }

    /**
     * Gets the value of the collectionTable property.
     * 
     * @return
     *     possible object is
     *     {@link CollectionTable }
     *     
     */
    public CollectionTable getCollectionTable() {
        return collectionTable;
    }

    /**
     * Sets the value of the collectionTable property.
     * 
     * @param value
     *     allowed object is
     *     {@link CollectionTable }
     *     
     */
    public void setCollectionTable(CollectionTable value) {
        this.collectionTable = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the targetClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetClass() {
        return targetClass;
    }

    /**
     * Sets the value of the targetClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetClass(String value) {
        this.targetClass = value;
    }

    /**
     * Gets the value of the fetch property.
     * 
     * @return
     *     possible object is
     *     {@link FetchType }
     *     
     */
    public FetchType getFetch() {
        return fetch;
    }

    /**
     * Sets the value of the fetch property.
     * 
     * @param value
     *     allowed object is
     *     {@link FetchType }
     *     
     */
    public void setFetch(FetchType value) {
        this.fetch = value;
    }

    /**
     * Gets the value of the access property.
     * 
     * @return
     *     possible object is
     *     {@link AccessType }
     *     
     */
    public AccessType getAccess() {
        return access;
    }

    /**
     * Sets the value of the access property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessType }
     *     
     */
    public void setAccess(AccessType value) {
        this.access = value;
    }

}
