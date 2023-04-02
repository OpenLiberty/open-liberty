/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa.tests.jpa31.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class QueryEntity {
    @Id
    private int id;

    private int intVal;
    private long longVal;
    private float floatVal;
    private double doubleVal;

    private String strVal;

    public QueryEntity() {

    }

    /**
     * @param id
     * @param intVal
     * @param longVal
     * @param floatVal
     * @param doubleVal
     * @param strVal
     */
    public QueryEntity(int id, int intVal, long longVal, float floatVal, double doubleVal, String strVal) {
        super();
        this.id = id;
        this.intVal = intVal;
        this.longVal = longVal;
        this.floatVal = floatVal;
        this.doubleVal = doubleVal;
        this.strVal = strVal;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the intVal
     */
    public int getIntVal() {
        return intVal;
    }

    /**
     * @param intVal the intVal to set
     */
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    /**
     * @return the longVal
     */
    public long getLongVal() {
        return longVal;
    }

    /**
     * @param longVal the longVal to set
     */
    public void setLongVal(long longVal) {
        this.longVal = longVal;
    }

    /**
     * @return the floatVal
     */
    public float getFloatVal() {
        return floatVal;
    }

    /**
     * @param floatVal the floatVal to set
     */
    public void setFloatVal(float floatVal) {
        this.floatVal = floatVal;
    }

    /**
     * @return the doubleVal
     */
    public double getDoubleVal() {
        return doubleVal;
    }

    /**
     * @param doubleVal the doubleVal to set
     */
    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    /**
     * @return the strVal
     */
    public String getStrVal() {
        return strVal;
    }

    /**
     * @param strVal the strVal to set
     */
    public void setStrVal(String strVal) {
        this.strVal = strVal;
    }

}
