/*******************************************************************************
 * Copyright (c) 1998, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.persistence;

import java.util.*;
import javax.ejb.EJBObject;

// FIX ME : Currently no difference between lazy and greedy modes on collections
// Lazy can be optimized by implementing a "smarter" iterator which will work
// off the lazy FinderEnumerator.

public class FinderCollectionIterator implements Iterator
{

    public FinderCollectionIterator(EJBObject[] elements)
    {
        this.elements = elements;
        this.index = 0;
    }

    public boolean hasNext()
    {
        if (elements != null)
            return (index < elements.length);
        else
            return false;
    }

    public Object next()
    {
        if ((elements != null) && (index < elements.length))
            return (elements[index++]);
        else
            throw new NoSuchElementException();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private EJBObject[] elements;
    private int index = 0;

}
