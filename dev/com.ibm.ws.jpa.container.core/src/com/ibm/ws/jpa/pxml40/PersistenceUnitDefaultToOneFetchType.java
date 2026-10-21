/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 *******************************************************************************/
package com.ibm.ws.jpa.pxml40;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for persistence-unit-default-to-one-fetch-type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 *
 * <pre>
 * &lt;simpleType name="persistence-unit-default-to-one-fetch-type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *     &lt;enumeration value="LAZY"/>
 *     &lt;enumeration value="EAGER"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 * New in JPA 4.0 - controls the default fetch type for to-one (OneToOne, ManyToOne) associations.
 */
@XmlType(name = "persistence-unit-default-to-one-fetch-type")
@XmlEnum
public enum PersistenceUnitDefaultToOneFetchType {

    LAZY,
    EAGER;

    public String value() {
        return name();
    }

    public static PersistenceUnitDefaultToOneFetchType fromValue(String v) {
        return valueOf(v);
    }

}
