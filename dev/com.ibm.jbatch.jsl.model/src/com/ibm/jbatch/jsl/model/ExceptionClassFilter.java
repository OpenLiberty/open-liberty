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
public abstract class ExceptionClassFilter {

    /**
     * Gets the value of the includeList property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the includeList property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getIncludeList().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ExceptionClassFilter.Include }
     *
     *
     */
    abstract public List<? extends Include> getIncludeList();

    /**
     * Gets the value of the excludeList property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the excludeList property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getExcludeList().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ExceptionClassFilter.Exclude }
     *
     *
     */
    abstract public List<? extends ExceptionClassFilter.Exclude> getExcludeList();

    public abstract static class Exclude {

        /**
         * Gets the value of the clazz property.
         *
         * @return
         *         possible object is
         *         {@link String }
         *
         */
        abstract public String getClazz();

        /**
         * Sets the value of the clazz property.
         *
         * @param value
         *            allowed object is
         *            {@link String }
         *
         */
        abstract public void setClazz(String value);

    }

    public abstract static class Include {

        /**
         * Gets the value of the clazz property.
         *
         * @return
         *         possible object is
         *         {@link String }
         *
         */
        abstract public String getClazz();

        /**
         * Sets the value of the clazz property.
         *
         * @param value
         *            allowed object is
         *            {@link String }
         *
         */
        abstract public void setClazz(String value);

    }
}