/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.samples.jaxws.catalog.stubs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Request wrapper for the add operation
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "add", propOrder = {
                                     "value1",
                                     "value2"
})
public class Add {

    @XmlElement(required = true)
    protected int value1;

    @XmlElement(required = true)
    protected int value2;

    /**
     * Gets the value of the value1 property.
     */
    public int getValue1() {
        return value1;
    }

    /**
     * Sets the value of the value1 property.
     */
    public void setValue1(int value) {
        this.value1 = value;
    }

    /**
     * Gets the value of the value2 property.
     */
    public int getValue2() {
        return value2;
    }

    /**
     * Sets the value of the value2 property.
     */
    public void setValue2(int value) {
        this.value2 = value;
    }

}
