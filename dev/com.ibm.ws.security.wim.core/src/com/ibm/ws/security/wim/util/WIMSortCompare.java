/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.util;

import java.util.Comparator;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.SortControl;

/**
 * Sorting comparable class.
 */
@Trivial
public class WIMSortCompare<T> implements Comparator<T> {
    SortControl sortControl = null;

    /**
     * Construct an WIMSortCompare
     */
    public WIMSortCompare(SortControl sortControl) {
        this.sortControl = sortControl;
    }

    /**
     * Compares its two objects for order. Returns a negative integer, zero, or
     * a positive integer as the first argument is less than, equal to, or greater than the second.
     * 
     * @param obj1 the first object
     * @param obj2 the second object
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     *         equal to, or greater than the second.
     * 
     */
    @Override
    public int compare(T entity1, T entity2) {
        SortHandler shandler = new SortHandler(sortControl);
        return shandler.compareEntitysWithRespectToProperties((Entity) entity1, (Entity) entity2);

    }
}
