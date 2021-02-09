/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.core;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ResourceAdapter;
import javax.sql.DataSource;

import com.ibm.ws.ejbcontainer.fat.rar.jdbc.JdbcDataSource;
import com.ibm.ws.ejbcontainer.fat.rar.spi.ManagedConnectionFactoryImpl;

/**
 * <p>This is the helper class for FVT resource adapter. </p>
 */
public class FVTAdapterHelper {
    private final static String CLASSNAME = AdapterUtil.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

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
     * @param jndiString DataSource JNDI name
     *
     * @return a datasource instance
     */
    public static DataSource getConnectionFactory(String jndiString) throws NamingException, ClassNotFoundException {
        svLogger.entering(CLASSNAME, "FVTAdapterHelper.getConnectionFactory", jndiString); // 313344.1
        DataSource specificDataSource = null;
        specificDataSource = (DataSource) getInitialContext().lookup(jndiString);
        svLogger.exiting(CLASSNAME, "FVTAdapterHelper.getConnectionFactory", specificDataSource); // 313344.1

        return specificDataSource;
    }

    /**
     * Get the initial context for JNDI namespace.
     *
     * @return An initial context.
     */
    private static InitialContext getInitialContext() throws NamingException {
        if (ic == null) {
            //        Properties props = new Properties();
            //        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.websphere.naming.WsnInitialContextFactory");
            ic = new InitialContext();
        }

        return ic;
    }

    /**
     * Get the resource adapter instance from the datasource object
     */
    public static ResourceAdapter getResourceAdapter(DataSource ds) {
        svLogger.entering(CLASSNAME, "FVTAdapterHelper.getResourceAdapter", ds);
        ResourceAdapter adapter = null;

        try {
            // start 313344.1
            DataSource dstemp = ds;
            svLogger.info("getResourceAdapter - dstemp: " + ds);

            JdbcDataSource jdbcdstemp = (JdbcDataSource) ds;
            svLogger.info("getResourceAdapter - jdbcdstemp: " + jdbcdstemp);

            ManagedConnectionFactoryImpl mcfimpl = jdbcdstemp.getMcf();
            svLogger.info("getResourceAdapter - mcfimpl: " + mcfimpl);

            ResourceAdapter adatemp = mcfimpl.getResourceAdapter();
            svLogger.info("getResourceAdapter - adatemp: " + adatemp);
            // end 313344.1
            adapter = ((JdbcDataSource) ds).getMcf().getResourceAdapter();
        } catch (ClassCastException cce) {
            svLogger.exiting(CLASSNAME, "FVTAdapterHelper.getResourceAdapter", "Exception" + cce.getMessage());
            throw cce;
        }

        svLogger.exiting(CLASSNAME, "FVTAdapterHelper.getResourceAdapter", adapter);
        return adapter;
    }
}