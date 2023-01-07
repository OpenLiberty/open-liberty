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
public abstract class Next implements com.ibm.jbatch.jsl.model.helper.TransitionElement {

    /**
     * Gets the value of the to property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getTo();

    /**
     * Sets the value of the to property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setTo(String value);

}