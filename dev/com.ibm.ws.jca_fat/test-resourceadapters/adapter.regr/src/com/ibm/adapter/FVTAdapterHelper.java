/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
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
import javax.resource.spi.ResourceAdapter;
import javax.sql.DataSource;

import com.ibm.adapter.jdbc.JdbcDataSource;
import com.ibm.adapter.message.FVTMessageProviderImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * This is the helper class for FVT resource adapter.
 * </p>
 */
public class FVTAdapterHelper {
    private static final TraceComponent tc = Tr
                    .register(FVTMessageProviderImpl.class);

    /** JNDI name space initial context */
    private static InitialContext ic;

    /**
     * Constructor for FVTAdapterHelper.
     */
    public FVTAdapterHelper() {}

    /**
     * Get the datasource for the jndi name given by jndiString.
     *
     * @param jndiString
     *            DataSource JNDI name
     *
     * @return a datasource instance
     */
    public static javax.sql.DataSource getConnectionFactory(String jndiString) throws javax.naming.NamingException, ClassNotFoundException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FVTAdapterHelper.getConnectionFactory", jndiString); // 313344.1

        javax.sql.DataSource specificDataSource = InitialContext.doLookup(jndiString);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FVTAdapterHelper.getConnectionFactory",
                    specificDataSource); // 313344.1

        return specificDataSource;

    }

    /**
     * Get the resource adapter instance from the datasource object
     */
    public static ResourceAdapter getResourceAdapter(DataSource ds) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FVTAdapterHelper.getResourceAdapter", ds);

        ResourceAdapter adapter = null;

        try {
            // start 313344.1
            DataSource dstemp = ds;
            Tr.event(tc, "getResourceAdapter", "dstemp: " + ds);

            JdbcDataSource jdbcdstemp = (JdbcDataSource) ds;
            Tr.event(tc, "getResourceAdapter", "jdbcdstemp: " + jdbcdstemp);

            com.ibm.adapter.spi.ManagedConnectionFactoryImpl mcfimpl = jdbcdstemp
                            .getMcf();
            Tr.event(tc, "getResourceAdapter", "mcfimpl: " + mcfimpl);

            ResourceAdapter adatemp = mcfimpl.getResourceAdapter();
            Tr.event(tc, "getResourceAdapter", "adatemp: " + adatemp);
            // end 313344.1
            adapter = ((JdbcDataSource) ds).getMcf().getResourceAdapter();
        } catch (ClassCastException cce) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "FVTAdapterHelper.getResourceAdapter", "Exception"
                                                                   + cce.getMessage());
            throw cce;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FVTAdapterHelper.getResourceAdapter", adapter);

        return adapter;
    }

}
