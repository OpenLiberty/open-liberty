/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.pxml20;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for persistence-unit-validation-mode-type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="persistence-unit-validation-mode-type">
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 * &lt;enumeration value="AUTO"/>
 * &lt;enumeration value="CALLBACK"/>
 * &lt;enumeration value="NONE"/>
 * &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "persistence-unit-validation-mode-type", namespace = "http://java.sun.com/xml/ns/persistence")
@XmlEnum
public enum PersistenceUnitValidationModeType {

    AUTO,
    CALLBACK,
    NONE;

    public String value() {
        return name();
    }

    public static PersistenceUnitValidationModeType fromValue(String v) {
        return valueOf(v);
    }

}
