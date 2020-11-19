/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>This class is an object cache to store the work instances.</p>
 */
public class FVTObjectCache {
    private static final TraceComponent tc = Tr.register(FVTObjectCache.class);
    public static final FVTObjectCache cache = new FVTObjectCache();

    // XAResource object.
    private final int maxXAResources = 10;
    private int numXAResources;

    // FVTComplexWorkImpl object.
    private final int maxComplexWorks = 10;
    private int numComplexWorks;

    // FVTSimplWorkImpl object.
    private final int maxSimpleWorks = 10;
    private int numSimpleWorks;

    FVTObjectCache() {
    }

    /**
     * @return the singleton ObjectCache instance.
     */
    public static final FVTObjectCache get() {
        return cache;
    }

}
