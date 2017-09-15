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

package com.ibm.ws.pmi.server;

public class PmiAttribute implements java.io.Serializable {
    private static final long serialVersionUID = 4849590234346163606L;
    String name;
    long value;

    public PmiAttribute(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return value;
    }

    /**
     * Define all the names available in PmiAttribute.
     */
    public final static String NUM_THREADS_IN_POOL = "NumThreadsInPool",
                         NUM_CREATED_THREADS = "NumCreatedThreads",
                         CONNECTION_POOL_SIZE = "ConnectionPoolSize";

}
