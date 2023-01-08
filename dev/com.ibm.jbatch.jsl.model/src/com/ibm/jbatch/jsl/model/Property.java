/*
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
*/
package com.ibm.jbatch.jsl.model;

/**
 *
 */
public abstract class Property {

    /**
     * Gets the value of the name property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    abstract public String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    abstract public void setName(String value);

    /**
     * Gets the value of the value property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    abstract public String getValue();

    /**
     * Sets the value of the value property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    abstract public void setValue(String value);

}