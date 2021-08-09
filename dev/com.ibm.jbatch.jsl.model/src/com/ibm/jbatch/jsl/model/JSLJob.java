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

import com.ibm.jbatch.jsl.model.helper.ExecutionElement;

/**
 *
 */
public abstract class JSLJob {

    /**
     * Gets the value of the properties property.
     *
     * @return
     *         possible object is
     *         {@link JSLProperties }
     *
     */
    abstract public JSLProperties getProperties();

    /**
     * Sets the value of the properties property.
     *
     * @param value
     *            allowed object is
     *            {@link JSLProperties }
     *
     */
    abstract public void setProperties(JSLProperties value);

    /**
     * Gets the value of the listeners property.
     *
     * @return
     *         possible object is
     *         {@link Listeners }
     *
     */
    abstract public Listeners getListeners();

    /**
     * Sets the value of the listeners property.
     *
     * @param value
     *            allowed object is
     *            {@link Listeners }
     *
     */
    abstract public void setListeners(Listeners value);

    /**
     * Gets the value of the executionElements property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the executionElements property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getExecutionElements().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Decision }
     * {@link Step }
     * {@link Split }
     * {@link Flow }
     *
     *
     */
    abstract public List<ExecutionElement> getExecutionElements();

    /**
     * Gets the value of the version property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getVersion();

    /**
     * Sets the value of the version property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setVersion(String value);

    /**
     * Gets the value of the id property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getId();

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
     * Gets the value of the restartable property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getRestartable();

    /**
     * Sets the value of the restartable property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setRestartable(String value);

}