/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

/**
 * This type was created in VisualAge.
 */
@SuppressWarnings("unchecked")
public class ArrayEnumeration implements java.util.Enumeration {
    private final Object[] _array;
    private int _index = 0;

    /**
     * ArrayEnumeration constructor comment.
     */
    public ArrayEnumeration(Object[] array) {
        _array = array;
    }

    /**
     * hasMoreElements method comment.
     */
    public boolean hasMoreElements() {
        if (_array == null) {
            return false;
        } else {
            synchronized (this) {
                return _index < _array.length;
            }
        }
    }

    /**
     * nextElement method comment.
     */
    public Object nextElement() {
        if (_array == null) {
            return null;
        } else {
            synchronized (this) {
                if (_index < _array.length) {
                    Object obj = _array[_index];
                    _index++;
                    return obj;
                } else {
                    return null;
                }
            }
        }
    }
}
