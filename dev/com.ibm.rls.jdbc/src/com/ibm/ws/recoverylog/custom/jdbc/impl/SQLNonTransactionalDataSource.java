/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.custom.jdbc.impl;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.TraceConstants;
import com.ibm.wsspi.resource.ResourceFactory;

//------------------------------------------------------------------------------
// Class: SQLNonTransactionalDataSource
//------------------------------------------------------------------------------
/**
 * <p>
 * The SQLNonTransactionalDataSource class provides a wrapper for the java.sql.DataSource
 * object that represents the special non-transactional data source that has been defined
 * by an administrator for storing Transaction Logs.
 * </p>
 *
 * <p>
 * The Liberty implementation relies on Declarative Services to coordinate the initialisation
 * of the Transaction and DataSource (com.ibm.ws.jdbc) components.
 * </p>
 */
public class SQLNonTransactionalDataSource {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(SQLNonTransactionalDataSource.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    //private NonTransactionalDataSource nonTranDataSource;
    DataSource nonTranDataSource = null;

    private CustomLogProperties _customLogProperties = null;

    //------------------------------------------------------------------------------
    // Method: SQLNonTransactionalDataSource.SQLNonTransactionalDataSource
    //------------------------------------------------------------------------------
    /**
     * <p> Constructor for the creation of
     * SQLNonTransactionalDataSource objects.
     * </p>
     *
     * @param dsName              The name of the Data Source.
     * @param customLogProperties The custom properties of the log.
     */
    public SQLNonTransactionalDataSource(String dsName, CustomLogProperties customLogProperties) {
        _customLogProperties = customLogProperties;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting CustomLogProperties in constructor" + customLogProperties);
    }

    //------------------------------------------------------------------------------
    // Method: SQLNonTransactionalDataSource.getDataSource
    //------------------------------------------------------------------------------
    /**
     * Locates a DataSource in config
     *
     * @return The DataSource.
     *
     * @exception
     */
    @FFDCIgnore(Exception.class)
    public DataSource getDataSource() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getDataSource");

        // Retrieve the data source factory from the CustomLogProperties. This Factory should be set in the JTMConfigurationProvider
        // by the jdbc component using DeclarativeServices. TxRecoveryAgentImpl gets the factory from the ConfigurationProvider and
        // then sets it into CustomLogProperties.
        ResourceFactory dataSourceFactory = _customLogProperties.resourceFactory();

        if (dataSourceFactory != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Using DataSourceFactory " + dataSourceFactory);
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getDataSource", "Null ResourceFactory InternalLogException");
            throw new InternalLogException("Failed to locate DataSource, null Resourcefactory", null);
        }

        try {
            nonTranDataSource = (DataSource) dataSourceFactory.createResource(null);
        } catch (Exception e) {
            //e.printStackTrace();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getDataSource", "Caught exception " + e + "throw InternalLogException");
            throw new InternalLogException("Failed to locate DataSource, caught exception ", null);
        }

/*
 * TEMPORARY This is waiting on fixes to DeclarativeServices which impact the jdbc component. At present it is
 * possible that the DataSource will have been set but that its associated jdbc driver service will still be initialising
 */
//        boolean refSet = false;
//        while (!refSet)
//        {
//            if (tc.isDebugEnabled())
//                Tr.debug(tc, "getDataSource after sleep");
//            try {
//
//                nonTranDataSource = (DataSource) dataSourceFactory.createResource(null);
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "Non Tran dataSource is " + nonTranDataSource);
//                Connection conn = nonTranDataSource.getConnection();
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "Established connection " + conn);
//
//                DatabaseMetaData mdata = conn.getMetaData();
//
//                String dbName = mdata.getDatabaseProductName();
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "Database name " + dbName);
//
//                String dbVersion = mdata.getDatabaseProductVersion();
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "Database version " + dbVersion);
//                refSet = true;
//            } catch (Exception e) {
//                // We will catch an exception if the DataSource is not yet fully formed
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "Caught exception: " + e);
//            }
//
//            if (!refSet)
//                Thread.sleep(200);
//        }
// eof TEMPORARY

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getDataSource", nonTranDataSource);
        return nonTranDataSource;
    }

}
