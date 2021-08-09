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

import java.util.List;

/**
 *
 */
public abstract class Split implements com.ibm.jbatch.jsl.model.helper.ExecutionElement {

    /**
     * Gets the value of the flows property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the flows property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getFlows().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Flow }
     *
     *
     */
    abstract public List<? extends Flow> getFlows();

    /**
     * Sets the value of the id property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setId(String value);

    /**
     * Gets the value of the nextFromAttribute property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getNextFromAttribute();

    /**
     * Sets the value of the nextFromAttribute property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setNextFromAttribute(String value);
}