/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.pxml10;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
 * &lt;element name="persistence-unit" maxOccurs="unbounded" minOccurs="0">
 * &lt;complexType>
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;element name="provider" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;element name="jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;element name="non-jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;element name="mapping-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="jar-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="class" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="exclude-unlisted-classes" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 * &lt;element name="properties" minOccurs="0">
 * &lt;complexType>
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="property" maxOccurs="unbounded" minOccurs="0">
 * &lt;complexType>
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * &lt;/element>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * &lt;/element>
 * &lt;/sequence>
 * &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="transaction-type" type="{http://java.sun.com/xml/ns/persistence}persistence-unit-transaction-type" />
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * &lt;/element>
 * &lt;/sequence>
 * &lt;attribute name="version" use="required" type="{http://java.sun.com/xml/ns/persistence}versionType" fixed="1.0" />
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
                                 "persistenceUnit"
})
@XmlRootElement(name = "persistence")
public class Persistence {

    @XmlElement(name = "persistence-unit")
    protected List<Persistence.PersistenceUnit> persistenceUnit;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String version;

    /**
     * Gets the value of the persistenceUnit property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the persistenceUnit property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getPersistenceUnit().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Persistence.PersistenceUnit }
     * 
     * 
     */
    public List<Persistence.PersistenceUnit> getPersistenceUnit() {
        if (persistenceUnit == null) {
            persistenceUnit = new ArrayList<Persistence.PersistenceUnit>();
        }
        return this.persistenceUnit;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getVersion() {
        if (version == null) {
            return "1.0";
        } else {
            return version;
        }
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * 
     * 
     * Configuration of a persistence unit.
     * 
     * 
     * 
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     * &lt;complexContent>
     * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     * &lt;sequence>
     * &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     * &lt;element name="provider" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     * &lt;element name="jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     * &lt;element name="non-jta-data-source" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
     * &lt;element name="mapping-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     * &lt;element name="jar-file" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     * &lt;element name="class" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
     * &lt;element name="exclude-unlisted-classes" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
     * &lt;element name="properties" minOccurs="0">
     * &lt;complexType>
     * &lt;complexContent>
     * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     * &lt;sequence>
     * &lt;element name="property" maxOccurs="unbounded" minOccurs="0">
     * &lt;complexType>
     * &lt;complexContent>
     * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     * &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     * &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     * &lt;/restriction>
     * &lt;/complexContent>
     * &lt;/complexType>
     * &lt;/element>
     * &lt;/sequence>
     * &lt;/restriction>
     * &lt;/complexContent>
     * &lt;/complexType>
     * &lt;/element>
     * &lt;/sequence>
     * &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     * &lt;attribute name="transaction-type" type="{http://java.sun.com/xml/ns/persistence}persistence-unit-transaction-type" />
     * &lt;/restriction>
     * &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
                                     "description",
                                     "provider",
                                     "jtaDataSource",
                                     "nonJtaDataSource",
                                     "mappingFile",
                                     "jarFile",
                                     "clazz",
                                     "excludeUnlistedClasses",
                                     "properties"
    })
    public static class PersistenceUnit {

        protected String description;
        protected String provider;
        @XmlElement(name = "jta-data-source")
        protected String jtaDataSource;
        @XmlElement(name = "non-jta-data-source")
        protected String nonJtaDataSource;
        @XmlElement(name = "mapping-file")
        protected List<String> mappingFile;
        @XmlElement(name = "jar-file")
        protected List<String> jarFile;
        @XmlElement(name = "class")
        protected List<String> clazz;
        @XmlElement(name = "exclude-unlisted-classes", defaultValue = "false")
        protected Boolean excludeUnlistedClasses;
        protected Persistence.PersistenceUnit.Properties properties;
        @XmlAttribute(required = true)
        protected String name;
        @XmlAttribute(name = "transaction-type")
        protected PersistenceUnitTransactionType transactionType;

        /**
         * Gets the value of the description property.
         * 
         * @return
         *         possible object is {@link String }
         * 
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the value of the description property.
         * 
         * @param value
         *            allowed object is {@link String }
         * 
         */
        public void setDescription(String value) {
            this.description = value;
        }

        /**
         * Gets the value of the provider property.
         * 
         * @return
         *         possible object is {@link String }
         * 
         */
        public String getProvider() {
            return provider;
        }

        /**
         * Sets the value of the provider property.
         * 
         * @param value
         *            allowed object is {@link String }
         * 
         */
        public void setProvider(String value) {
            this.provider = value;
        }

        /**
         * Gets the value of the jtaDataSource property.
         * 
         * @return
         *         possible object is {@link String }
         * 
         */
        public String getJtaDataSource() {
            return jtaDataSource;
        }

        /**
         * Sets the value of the jtaDataSource property.
         * 
         * @param value
         *            allowed object is {@link String }
         * 
         */
        public void setJtaDataSource(String value) {
            this.jtaDataSource = value;
        }

        /**
         * Gets the value of the nonJtaDataSource property.
         * 
         * @return
         *         possible object is {@link String }
         * 
         */
        public String getNonJtaDataSource() {
            return nonJtaDataSource;
        }

        /**
         * Sets the value of the nonJtaDataSource property.
         * 
         * @param value
         *            allowed object is {@link String }
         * 
         */
        public void setNonJtaDataSource(String value) {
            this.nonJtaDataSource = value;
        }

        /**
         * Gets the value of the mappingFile property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the mappingFile property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         * getMappingFile().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list {@link String }
         * 
         * 
         */
        public List<String> getMappingFile() {
            if (mappingFile == null) {
                mappingFile = new ArrayList<String>();
            }
            return this.mappingFile;
        }

        /**
         * Gets the value of the jarFile property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the jarFile property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         * getJarFile().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list {@link String }
         * 
         * 
         */
        public List<String> getJarFile() {
            if (jarFile == null) {
                jarFile = new ArrayList<String>();
            }
            return this.jarFile;
        }

        /**
         * Gets the value of the clazz property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the clazz property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         * getClazz().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list {@link String }
         * 
         * 
         */
        public List<String> getClazz() {
            if (clazz == null) {
                clazz = new ArrayList<String>();
            }
            return this.clazz;
        }

        /**
         * Gets the value of the excludeUnlistedClasses property.
         * 
         * @return
         *         possible object is {@link Boolean }
         * 
         */
        public Boolean isExcludeUnlistedClasses() {
            return excludeUnlistedClasses;
        }

        /**
         * Sets the value of the excludeUnlistedClasses property.
         * 
         * @param value
         *            allowed object is {@link Boolean }
         * 
         */
        public void setExcludeUnlistedClasses(Boolean value) {
            this.excludeUnlistedClasses = value;
        }

        /**
         * Gets the value of the properties property.
         * 
         * @return
         *         possible object is {@link Persistence.PersistenceUnit.Properties }
         * 
         */
        public Persistence.PersistenceUnit.Properties getProperties() {
            return properties;
        }

        /**
         * Sets the value of the properties property.
         * 
         * @param value
         *            allowed object is {@link Persistence.PersistenceUnit.Properties }
         * 
         */
        public void setProperties(Persistence.PersistenceUnit.Properties value) {
            this.properties = value;
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
         *            allowed object is {@link String }
         * 
         */
        public void setName(String value) {
            this.name = value;
        }

        /**
         * Gets the value of the transactionType property.
         * 
         * @return
         *         possible object is {@link PersistenceUnitTransactionType }
         * 
         */
        public PersistenceUnitTransactionType getTransactionType() {
            return transactionType;
        }

        /**
         * Sets the value of the transactionType property.
         * 
         * @param value
         *            allowed object is {@link PersistenceUnitTransactionType }
         * 
         */
        public void setTransactionType(PersistenceUnitTransactionType value) {
            this.transactionType = value;
        }

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
         * &lt;element name="property" maxOccurs="unbounded" minOccurs="0">
         * &lt;complexType>
         * &lt;complexContent>
         * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         * &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
         * &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
         * &lt;/restriction>
         * &lt;/complexContent>
         * &lt;/complexType>
         * &lt;/element>
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
                                         "property"
        })
        public static class Properties {

            protected List<Persistence.PersistenceUnit.Properties.Property> property;

            /**
             * Gets the value of the property property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the property property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             * getProperty().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list {@link Persistence.PersistenceUnit.Properties.Property }
             * 
             * 
             */
            public List<Persistence.PersistenceUnit.Properties.Property> getProperty() {
                if (property == null) {
                    property = new ArrayList<Persistence.PersistenceUnit.Properties.Property>();
                }
                return this.property;
            }

            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             * &lt;complexContent>
             * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             * &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
             * &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
             * &lt;/restriction>
             * &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class Property {

                @XmlAttribute(required = true)
                protected String name;
                @XmlAttribute(required = true)
                protected String value;

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
                 *            allowed object is {@link String }
                 * 
                 */
                public void setName(String value) {
                    this.name = value;
                }

                /**
                 * Gets the value of the value property.
                 * 
                 * @return
                 *         possible object is {@link String }
                 * 
                 */
                public String getValue() {
                    return value;
                }

                /**
                 * Sets the value of the value property.
                 * 
                 * @param value
                 *            allowed object is {@link String }
                 * 
                 */
                public void setValue(String value) {
                    this.value = value;
                }

            }

        }

    }

}
