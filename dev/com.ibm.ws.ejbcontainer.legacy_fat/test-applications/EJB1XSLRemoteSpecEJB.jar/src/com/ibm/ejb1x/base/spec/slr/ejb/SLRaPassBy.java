/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.slr.ejb;

/**
 * Key class for Entity Bean: SLRa
 */
public class SLRaPassBy implements java.io.Serializable {
    private static final long serialVersionUID = 4442732046252207204L;
    public String key;
    public String key2;
    public int value;

    /**
     * Creates an empty key for Entity Bean: SLRa
     */
    public SLRaPassBy(String initKey, String initKey2, int initValue) {
        key = initKey;
        key2 = initKey2;
        value = initValue;
    }

    /**
     * Returns true if both keys are equal.
     */
    @Override
    public boolean equals(java.lang.Object otherKey) {
        if (otherKey instanceof SLRaPassBy) {
            SLRaPassBy o = (SLRaPassBy) otherKey;
            return (this.key.equals(o.key) && this.key2.equals(o.key2) && this.value == o.value);
        }
        return false;
    }

    /**
     * Returns the hash code for the key.
     */
    @Override
    public int hashCode() {
        return (this.key.hashCode() + value);
    }

    /**
     * Get the value.
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the value.
     */
    public void setKey(String newKey) {
        key = newKey;
    }

    /**
     * Get the value.
     */
    public String getKey2() {
        return key2;
    }

    /**
     * Set the value.
     */
    public void setKey2(String newKey) {
        key2 = newKey;
    }

    /**
     * Get the value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Set the value.
     */
    public void setValue(int newValue) {
        value = newValue;
    }
}
