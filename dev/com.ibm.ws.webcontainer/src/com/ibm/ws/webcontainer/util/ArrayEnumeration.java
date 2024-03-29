/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.util;

/**
 * This type was created in VisualAge.
 */
@SuppressWarnings("unchecked")
public class ArrayEnumeration implements java.util.Enumeration
{
    private Object[] _array;
    private int _index = 0;

    /**
     * ArrayEnumeration constructor comment.
     */
    public ArrayEnumeration(Object[] array) 
    {
        _array = array;
    }

    /**
     * hasMoreElements method comment.
     */
    public boolean hasMoreElements() 
    {
        if (_array == null)
        {
            return false;
        }
        else
        {
            return _index < _array.length;
        }
    }
    
    /**
     * nextElement method comment.
     */
    public Object nextElement() 
    {
        if (_array == null)
        {
            return null;
        }
        else
        {
            synchronized(this){
                if (_index < _array.length)
                {
                    Object obj = _array[_index];
                    _index++;
                    return obj;
                }
                else
                {
                    return null;
                }
            }
        }
    }
}
