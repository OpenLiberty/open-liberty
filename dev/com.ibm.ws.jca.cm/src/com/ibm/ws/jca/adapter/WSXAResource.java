/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.adapter;

import javax.resource.spi.ManagedConnection;
import javax.transaction.xa.XAResource;

/**
 * XA resource that is associated with a ManagedConnection.
 */
public interface WSXAResource extends XAResource
{
    /**
     * Returns the managed connection that created this XA resource.
     * 
     * @return the managed connection that created this XA resource.
     */
    ManagedConnection getManagedConnection();
}