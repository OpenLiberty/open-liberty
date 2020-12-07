// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  06/25/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//  05/15/06   cjn        313344.1     Fix problems
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.core;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ResourceAdapter;
import javax.sql.DataSource;

import com.ibm.websphere.ejbcontainer.test.rar.jdbc.JdbcDataSource;
import com.ibm.websphere.ejbcontainer.test.rar.spi.ManagedConnectionFactoryImpl;

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
    public FVTAdapterHelper() {}

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