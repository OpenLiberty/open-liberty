package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * AbstractMapEntry is an internal class which provides an implementation
 * of Map.Entry.
 */
abstract class AbstractMapEntry
                extends ManagedObject
                implements Map.Entry
{

    Object key;
    Token value;

    interface Type
    {
        Object get(AbstractMapEntry entry);
    } // interface Type.

    AbstractMapEntry(Object theKey) {
        key = theKey;
    } // AbstractMapEntry().

    AbstractMapEntry(Object theKey,
                     Token theValue) {
        key = theKey;
        value = theValue;
    } // AbstractMapEntry().

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // No FFDC code needed.
            return null;
        } // try...
    } // clone().

    /**
     * @see com.ibm.ws.objectManager.Map.Entry#getKey()
     */
    public Object getKey() {
        return key;
    }

    /**
     * @see com.ibm.ws.objectManager.Map.Entry#getValue()
     */
    public Token getValue() {
        return value;
    }

    /**
     * Sets the value for this entry
     * 
     * @param object the new value
     * @return the previous value
     */
    public Object setValue(Token object) {
        Object result = value;
        value = object;
        return result;
    }
} // class AbstractMapEntry.
