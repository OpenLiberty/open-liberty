/*
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
*/
package com.ibm.jbatch.jsl.model;

/**
 *
 */
public abstract class End implements com.ibm.jbatch.jsl.model.helper.TransitionElement {

    /**
     * Gets the value of the exitStatus property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getExitStatus();

    /**
     * Sets the value of the exitStatus property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setExitStatus(String value);

}