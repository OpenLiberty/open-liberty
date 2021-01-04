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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * The icon type contains small-icon and large-icon elements that specify the file names for small and large
 * GIF, JPEG, or PNG icon images used to represent the parent element in a GUI tool. The xml:lang attribute
 * defines the language that the icon file names are provided in. Its value is "en" (English) by default.
 * <p>
 * Java class for iconType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="iconType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="small-icon" type="{http://java.sun.com/xml/ns/javaee}pathType" minOccurs="0"/>
 *         &lt;element name="large-icon" type="{http://java.sun.com/xml/ns/javaee}pathType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "iconType", propOrder = {
        "smallIcon", "largeIcon" })
public class IconType {

    @XmlElement(name = "small-icon")
    protected PathType smallIcon;
    @XmlElement(name = "large-icon")
    protected PathType largeIcon;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected java.lang.String id;
    @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
    protected java.lang.String lang;

    /**
     * Gets the value of the smallIcon property.
     *
     * @return possible object is {@link PathType }
     */
    public PathType getSmallIcon() {
        return smallIcon;
    }

    /**
     * Sets the value of the smallIcon property.
     *
     * @param value allowed object is {@link PathType }
     */
    public void setSmallIcon(PathType value) {
        this.smallIcon = value;
    }

    /**
     * Gets the value of the largeIcon property.
     *
     * @return possible object is {@link PathType }
     */
    public PathType getLargeIcon() {
        return largeIcon;
    }

    /**
     * Sets the value of the largeIcon property.
     *
     * @param value allowed object is {@link PathType }
     */
    public void setLargeIcon(PathType value) {
        this.largeIcon = value;
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

    /**
     * Gets the value of the lang property.
     *
     * @return possible object is {@link java.lang.String }
     */
    public java.lang.String getLang() {
        return lang;
    }

    /**
     * Sets the value of the lang property.
     *
     * @param value allowed object is {@link java.lang.String }
     */
    public void setLang(java.lang.String value) {
        this.lang = value;
    }

}
