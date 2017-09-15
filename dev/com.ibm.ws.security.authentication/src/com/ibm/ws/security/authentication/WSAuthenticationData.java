/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication;

import java.util.HashMap;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.util.ByteArray;

/**
 * This class represents the data to be used during authentication.
 * The authentication data are used by the AuthenticationService in order to
 * authenticate. The class provides for protecting sensitive data from being displayed
 * in traces when the key being used is identified as a sensitive one.
 */
public class WSAuthenticationData implements AuthenticationData {

    private final HashMap<String, Object> credentialsMap;
    private int hash = -1;

    public WSAuthenticationData() {
        this.credentialsMap = new HashMap<String, Object>();
    }

    /**
     * Sets the key-value pair.
     *
     * @param key
     * @param value
     */
    @Override
    public void set(String key, @Sensitive Object value) {
        Object tempValue = value;

        if (isSensitive(key)) {
            if (value == null) {
                tempValue = ProtectedString.NULL_PROTECTED_STRING;
            } else if (value instanceof String) {
                if (((String) value).isEmpty()) {
                    tempValue = ProtectedString.EMPTY_PROTECTED_STRING;
                } else {
                    tempValue = new ProtectedString(((String) value).toCharArray());
                }
            } else if (value instanceof char[]) {
                tempValue = new ProtectedString((char[]) value);
            } else if (value instanceof ProtectedString) {
                tempValue = value;
            } else {
                throw new IllegalArgumentException("Setting a password into WSAuthenticationData expects the stored Object to be either a String, char[] or ProtectedString. Instead, stored Object was of type "
                                                   + value.getClass().getName());
            }
        }

        if (value instanceof byte[]) {
            tempValue = new ByteArray((byte[]) value);
        }
        credentialsMap.put(key, tempValue);
        hash = -1;
    }

    /**
     * Retrieves the stored value for the key.
     *
     * @param key
     * @return
     */
    @Override
    @Sensitive
    public Object get(String key) {
        Object retrievedValue = credentialsMap.get(key);
        if (retrievedValue != null && isSensitive(key)) {
            retrievedValue = ((ProtectedString) retrievedValue).getChars();
        }

        if (retrievedValue instanceof ByteArray) {
            retrievedValue = ((ByteArray) retrievedValue).getArray();
        }
        return retrievedValue;
    }

    protected boolean isSensitive(String key) {
        return PASSWORD.equals(key);
    }

    @Override
    public String toString() {
        return credentialsMap.toString();
    }

    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof WSAuthenticationData == false) {
            return false;
        }
        return credentialsMap.equals(((WSAuthenticationData) otherObject).credentialsMap);
    }

    @Override
    public int hashCode() {
        if (hash == -1) {
            hash = credentialsMap.hashCode();
        }
        return hash;
    }
}
