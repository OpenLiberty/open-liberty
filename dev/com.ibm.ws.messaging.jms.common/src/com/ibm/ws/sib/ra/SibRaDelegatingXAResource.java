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
package com.ibm.ws.sib.ra;

import com.ibm.wsspi.sib.core.SIXAResource;

/**
 * Interface for XAResources which delegate to an <code>SIXAResource</code>.
 */
public interface SibRaDelegatingXAResource
{
    /**
     * Returns the delegated <code>SIXAResource</code>.
     * 
     * @return the <code>SIXAResource</code>
     */
    public SIXAResource getSiXaResource();
}
