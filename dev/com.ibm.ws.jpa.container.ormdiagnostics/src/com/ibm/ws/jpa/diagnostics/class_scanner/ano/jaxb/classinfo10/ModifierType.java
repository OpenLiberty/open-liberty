/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ModifierType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ModifierType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ABSTRACT"/>
 *     &lt;enumeration value="FINAL"/>
 *     &lt;enumeration value="NATIVE"/>
 *     &lt;enumeration value="PRIVATE"/>
 *     &lt;enumeration value="PROTECTED"/>
 *     &lt;enumeration value="PUBLIC"/>
 *     &lt;enumeration value="STATIC"/>
 *     &lt;enumeration value="SYNCHRONIZED"/>
 *     &lt;enumeration value="TRANSIENT"/>
 *     &lt;enumeration value="VOLATILE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ModifierType")
@XmlEnum
public enum ModifierType {


    /**
     * Corresponds with the java “abstract” keyword.
     * 
     */
    ABSTRACT,

    /**
     * Corresponds with the java “final” keyword.
     * 
     */
    FINAL,

    /**
     * Corresponds with the java “native” keyword.
     * 
     */
    NATIVE,

    /**
     * Corresponds with the java “private” keyword.
     * 
     */
    PRIVATE,

    /**
     * Corresponds with the java “protected” keyword.
     * 
     */
    PROTECTED,

    /**
     * Corresponds with the java “public” keyword.
     * 
     */
    PUBLIC,

    /**
     * Corresponds with the java “static” keyword.
     * 
     */
    STATIC,

    /**
     * Corresponds with the java “synchronized” keyword.
     * 
     */
    SYNCHRONIZED,

    /**
     * Corresponds with the java “transient” keyword.
     * 
     */
    TRANSIENT,

    /**
     * Corresponds with the java “volatile” keyword.
     * 
     */
    VOLATILE;

    public String value() {
        return name();
    }

    public static ModifierType fromValue(String v) {
        return valueOf(v);
    }

}
