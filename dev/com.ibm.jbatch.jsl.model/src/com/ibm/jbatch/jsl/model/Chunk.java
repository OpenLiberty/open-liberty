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
public abstract class Chunk {

    /**
     * Gets the value of the reader property.
     *
     * @return
     *         possible object is
     *         {@link ItemReader }
     *
     */
    abstract public ItemReader getReader();

    /**
     * Sets the value of the reader property.
     *
     * @param value
     *            allowed object is
     *            {@link ItemReader }
     *
     */
    abstract public void setReader(ItemReader value);

    /**
     * Gets the value of the processor property.
     *
     * @return
     *         possible object is
     *         {@link ItemProcessor }
     *
     */
    abstract public ItemProcessor getProcessor();

    /**
     * Sets the value of the processor property.
     *
     * @param value
     *            allowed object is
     *            {@link ItemProcessor }
     *
     */
    abstract public void setProcessor(ItemProcessor value);

    /**
     * Gets the value of the writer property.
     *
     * @return
     *         possible object is
     *         {@link ItemWriter }
     *
     */
    abstract public ItemWriter getWriter();

    /**
     * Sets the value of the writer property.
     *
     * @param value
     *            allowed object is
     *            {@link ItemWriter }
     *
     */
    abstract public void setWriter(ItemWriter value);

    /**
     * Gets the value of the checkpointAlgorithm property.
     *
     * @return
     *         possible object is
     *         {@link CheckpointAlgorithm }
     *
     */
    abstract public CheckpointAlgorithm getCheckpointAlgorithm();

    /**
     * Sets the value of the checkpointAlgorithm property.
     *
     * @param value
     *            allowed object is
     *            {@link CheckpointAlgorithm }
     *
     */
    abstract public void setCheckpointAlgorithm(CheckpointAlgorithm value);

    /**
     * Gets the value of the skippableExceptionClasses property.
     *
     * @return
     *         possible object is
     *         {@link ExceptionClassFilter }
     *
     */
    abstract public ExceptionClassFilter getSkippableExceptionClasses();

    /**
     * Sets the value of the skippableExceptionClasses property.
     *
     * @param value
     *            allowed object is
     *            {@link ExceptionClassFilter }
     *
     */
    abstract public void setSkippableExceptionClasses(ExceptionClassFilter value);

    /**
     * Gets the value of the retryableExceptionClasses property.
     *
     * @return
     *         possible object is
     *         {@link ExceptionClassFilter }
     *
     */
    abstract public ExceptionClassFilter getRetryableExceptionClasses();

    /**
     * Sets the value of the retryableExceptionClasses property.
     *
     * @param value
     *            allowed object is
     *            {@link ExceptionClassFilter }
     *
     */
    abstract public void setRetryableExceptionClasses(ExceptionClassFilter value);

    /**
     * Gets the value of the noRollbackExceptionClasses property.
     *
     * @return
     *         possible object is
     *         {@link ExceptionClassFilter }
     *
     */
    abstract public ExceptionClassFilter getNoRollbackExceptionClasses();

    /**
     * Sets the value of the noRollbackExceptionClasses property.
     *
     * @param value
     *            allowed object is
     *            {@link ExceptionClassFilter }
     *
     */
    abstract public void setNoRollbackExceptionClasses(ExceptionClassFilter value);

    /**
     * Gets the value of the checkpointPolicy property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getCheckpointPolicy();

    /**
     * Sets the value of the checkpointPolicy property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setCheckpointPolicy(String value);

    /**
     * Gets the value of the itemCount property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getItemCount();

    /**
     * Sets the value of the itemCount property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setItemCount(String value);

    /**
     * Gets the value of the timeLimit property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getTimeLimit();

    /**
     * Sets the value of the timeLimit property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setTimeLimit(String value);

    /**
     * Gets the value of the skipLimit property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getSkipLimit();

    /**
     * Sets the value of the skipLimit property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setSkipLimit(String value);

    /**
     * Gets the value of the retryLimit property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    abstract public String getRetryLimit();

    /**
     * Sets the value of the retryLimit property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     *
     */
    abstract public void setRetryLimit(String value);

}