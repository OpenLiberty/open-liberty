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
public abstract class Step implements com.ibm.jbatch.jsl.model.helper.ExecutionElement {

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
     * Gets the value of the batchlet property.
     *
     * @return
     *         possible object is
     *         {@link Batchlet }
     *
     */
    abstract public Batchlet getBatchlet();

    /**
     * Sets the value of the batchlet property.
     *
     * @param value
     *            allowed object is
     *            {@link Batchlet }
     *
     */
    abstract public void setBatchlet(Batchlet value);

    /**
     * Gets the value of the chunk property.
     *
     * @return
     *         possible object is
     *         {@link Chunk }
     *
     */
    abstract public Chunk getChunk();

    /**
     * Sets the value of the chunk property.
     *
     * @param value
     *            allowed object is
     *            {@link Chunk }
     *
     */
    abstract public void setChunk(Chunk value);

    /**
     * Gets the value of the partition property.
     *
     * @return
     *         possible object is
     *         {@link Partition }
     *
     */
    abstract public Partition getPartition();

    /**
     * Sets the value of the partition property.
     *
     * @param value
     *            allowed object is
     *            {@link Partition }
     *
     */
    abstract public void setPartition(Partition value);

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
     * Gets the value of the startLimit property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getStartLimit();

    /**
     * Sets the value of the startLimit property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setStartLimit(String value);

    /**
     * Gets the value of the allowStartIfComplete property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getAllowStartIfComplete();

    /**
     * Sets the value of the allowStartIfComplete property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setAllowStartIfComplete(String value);

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