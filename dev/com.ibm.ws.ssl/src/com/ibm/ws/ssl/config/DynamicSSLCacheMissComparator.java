/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.config;

import java.io.Serializable;

/**
 * DynamicSSLCacheMissComparator instance.
 * <p>
 * This class handles comparing two ConnectionInfo HashMaps for equality
 * in the TreeSet for cache misses during dynamic outbound SSL config decisions.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
@SuppressWarnings("rawtypes")
public class DynamicSSLCacheMissComparator implements java.util.Comparator, Serializable {

    private static final long serialVersionUID = -8929150182102517588L;

    /**
     * Constructor.
     */
    public DynamicSSLCacheMissComparator() {
        // do nothing
    }

    /**
     * Compares its two arguments for order.
     **/
    @Override
    public int compare(Object o1, Object o2) {
        return (o1.hashCode() - o2.hashCode());
    }
}
