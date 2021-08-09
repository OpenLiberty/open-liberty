/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.xml.ejb;

/**
 * Key class for a Results Bean.
 */
public class ResultsKey implements java.io.Serializable {
    private static final long serialVersionUID = -7714163158862400874L;

    /**
     * Implementation field for persistent attribute: pKey
     */
    public String pKey;

    /**
     * Creates an empty key for Stateful Bean: ResultsLocal
     */
    public ResultsKey() {
        pKey = "";
    }

    /**
     * Creates a key using value passed
     */
    public ResultsKey(String key) {
        pKey = key;
    }

    /**
     * Returns true if both keys are equal.
     */
    @Override
    public boolean equals(java.lang.Object otherKey) {
        if (otherKey instanceof ResultsKey) {
            ResultsKey o = (ResultsKey) otherKey;
            return (this.pKey.equals(o.pKey));
        }
        return false;
    }

    /**
     * Returns the hash code for the key.
     */
    @Override
    public int hashCode() {
        return (pKey.hashCode());
    }

    /**
     * Get accessor for persistent attribute: pKey
     */
    public java.lang.String getPKey() {
        return pKey;
    }

    /**
     * Set accessor for persistent attribute: pKey
     */
    public void setPKey(java.lang.String newPKey) {
        pKey = newPKey;
    }
}
