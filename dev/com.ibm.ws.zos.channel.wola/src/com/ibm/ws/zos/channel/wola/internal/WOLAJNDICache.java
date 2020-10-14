/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import javax.naming.NamingException;

/**
 * Lookup a WOLA EJB from a JNDI name.
 */
public interface WOLAJNDICache {

    /**
     * Lookup a WOLA EJB from a JNDI name.
     *
     * @param jndiNameBytes The JNDI name, as EBCDIC bytes.
     * @return The EJB stub
     * @throws NamingException Thrown if the naming service encounters an error.
     */
    public WOLAInboundTarget jndiLookup(byte[] jndiNameBytes) throws NamingException;

}
