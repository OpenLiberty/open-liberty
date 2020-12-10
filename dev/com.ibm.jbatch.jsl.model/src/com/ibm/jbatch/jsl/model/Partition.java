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
public abstract class Partition {

    /**
     * Gets the value of the mapper property.
     *
     * @return
     *         possible object is
     *         {@link PartitionMapper }
     *
     */
    abstract public PartitionMapper getMapper();

    /**
     * Sets the value of the mapper property.
     *
     * @param value
     *            allowed object is
     *            {@link PartitionMapper }
     *
     */
    abstract public void setMapper(PartitionMapper value);

    /**
     * Gets the value of the plan property.
     *
     * @return
     *         possible object is
     *         {@link PartitionPlan }
     *
     */
    abstract public PartitionPlan getPlan();

    /**
     * Sets the value of the plan property.
     *
     * @param value
     *            allowed object is
     *            {@link PartitionPlan }
     *
     */
    abstract public void setPlan(PartitionPlan value);

    /**
     * Gets the value of the collector property.
     *
     * @return
     *         possible object is
     *         {@link Collector }
     *
     */
    abstract public Collector getCollector();

    /**
     * Sets the value of the collector property.
     *
     * @param value
     *            allowed object is
     *            {@link Collector }
     *
     */
    abstract public void setCollector(Collector value);

    /**
     * Gets the value of the analyzer property.
     *
     * @return
     *         possible object is
     *         {@link Analyzer }
     *
     */
    abstract public Analyzer getAnalyzer();

    /**
     * Sets the value of the analyzer property.
     *
     * @param value
     *            allowed object is
     *            {@link Analyzer }
     *
     */
    abstract public void setAnalyzer(Analyzer value);

    /**
     * Gets the value of the reducer property.
     *
     * @return
     *         possible object is
     *         {@link PartitionReducer }
     *
     */
    abstract public PartitionReducer getReducer();

    /**
     * Sets the value of the reducer property.
     *
     * @param value
     *            allowed object is
     *            {@link PartitionReducer }
     *
     */
    abstract public void setReducer(PartitionReducer value);

}