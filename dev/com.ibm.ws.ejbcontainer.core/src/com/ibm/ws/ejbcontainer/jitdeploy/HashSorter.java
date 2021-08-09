/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.util.Comparator;

/**
 * Implementation of Comparator interface that will result in objects
 * being sorted in order based on hashCode. <p>
 * 
 * For objects which yeild the same hashCode value, they will be sorted
 * in 'natural' order. <p>
 * 
 * Null values in the collection are NOT supported. <p>
 * 
 * This implementation may be used for any classes or interfaces that
 * implement 'Comparable' (i.e. <T extends Comparable>)... and supports
 * sorting collections that contain subclasses of the specified type
 * if the subclass is comparable to the super class (i.e. <? super T>).
 */
final class HashSorter<T extends Comparable<? super T>> implements Comparator<T>
{
    public int compare(T o1, T o2)
    {
        int hash1 = o1.hashCode();
        int hash2 = o2.hashCode();

        if (hash1 < hash2)
            return -1;
        if (hash1 > hash2)
            return 1;

        return o1.compareTo(o2);
    }
}
