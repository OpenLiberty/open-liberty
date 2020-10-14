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

import javax.naming.InitialContext;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * This is the helper class for FVT resource adapter.
 * </p>
 */
public class FVTAdapterHelper {
    private static final TraceComponent tc = Tr
                    .register(FVTAdapterHelper.class);

    /** JNDI name space initial context */
    private static InitialContext ic;

    /**
     * Constructor for FVTAdapterHelper.
     */
    public FVTAdapterHelper() {
    }

    /**
     * Get the datasource for the jndi name given by jndiString.
     *
     * @param jndiString
     *                       DataSource JNDI name
     *
     * @return a datasource instance
     */
    public static javax.sql.DataSource getConnectionFactory(String jndiString) throws javax.naming.NamingException, ClassNotFoundException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FVTAdapterHelper.getConnectionFactory", jndiString); // 313344.1

        javax.sql.DataSource specificDataSource = null;

        specificDataSource = (javax.sql.DataSource) javax.rmi.PortableRemoteObject
                        .narrow(getInitialContext().lookup(jndiString),
                                javax.sql.DataSource.class);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FVTAdapterHelper.getConnectionFactory",
                    specificDataSource); // 313344.1

        return specificDataSource;

    }

    /**
     * Get the initial context for JNDI namespace.
     *
     * @return An initial context.
     */
    private static javax.naming.InitialContext getInitialContext() throws javax.naming.NamingException {

        if (ic == null) {
            java.util.Properties props = new java.util.Properties();
            props.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                      "com.ibm.websphere.naming.WsnInitialContextFactory");

            ic = new javax.naming.InitialContext();
        }

        return ic;
    }

}
