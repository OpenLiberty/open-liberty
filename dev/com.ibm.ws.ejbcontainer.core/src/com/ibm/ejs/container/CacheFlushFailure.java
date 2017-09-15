/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * This exception is thrown to indicate an error has occurred
 * while flushing the persistent state of an entity bean to the
 * persistent store. <p>
 */

public class CacheFlushFailure
                extends ContainerException
{
    private static final long serialVersionUID = -2647042063557834894L;

    /**
     * Create a new <code>CacheFlushFailure</code> instance. <p>
     */
    public CacheFlushFailure(Throwable ex) {
        super(ex);
    } // CacheFlushFailure

} // CacheFlushFailure
