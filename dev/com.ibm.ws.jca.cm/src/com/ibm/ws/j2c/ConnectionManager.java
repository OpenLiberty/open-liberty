/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.j2c;

import javax.resource.ResourceException;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.LazyEnlistableConnectionManager;

/*
 * Interface name   : ConnectionManager
 *
 * Scope            : EJB server, WEB server
 *
 * Object model     : 1 per each set of res-ref attributes for a deployed resource adapter
 *
 * This ConnectionManager interface introduces the getCMConfigData() method, which is only
 * to be used by the relational resource adapter.
 *
 */

public interface ConnectionManager extends LazyAssociatableConnectionManager, LazyEnlistableConnectionManager {

    public abstract com.ibm.ejs.j2c.CMConfigData getCMConfigData();

    /**
     * This method exposes the purging of the connection pool, it is expected to be called
     * only in the case of a database failover/reroute, where the old pool of connections
     * needs to be cleared because all connections are being targeted to a failover database.
     * 
     * @throws ResourceException
     */
    public void purgePool() throws ResourceException;

}
