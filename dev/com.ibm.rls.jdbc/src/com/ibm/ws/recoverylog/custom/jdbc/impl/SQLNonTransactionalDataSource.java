/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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
import com.ibm.wsspi.resource.ResourceConfig;
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
     * @throws InternalLogException
     *
     * @exception
     */
    @FFDCIgnore(Exception.class)
    public DataSource getDataSource() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getDataSource");

        // Retrieve the data source factory from the CustomLogProperties. This Factory should be set in the JTMConfigurationProvider
        // by the jdbc component using DeclarativeServices. TxRecoveryAgentImpl gets the factory from the ConfigurationProvider and
        // then sets it into CustomLogProperties.
        final ResourceFactory dataSourceFactory = _customLogProperties.resourceFactory();

        if (dataSourceFactory != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Using DataSourceFactory " + dataSourceFactory);
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getDataSource", "Null ResourceFactory InternalLogException");
            throw new InternalLogException("Failed to locate DataSource, null Resourcefactory", null);
        }

        try {
            // Retrieve the resourceConfig from the custom log properties. This may be null, in which case the "old"
            // behaviour will pertain with application authentication. A non-null resourceConfig will have been
            // configured if container authentication has been specified.
            ResourceConfig resourceConfig = _customLogProperties.resourceConfig();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "create resource with ResourceConfig ", resourceConfig);
            nonTranDataSource = (DataSource) dataSourceFactory.createResource(resourceConfig);
        } catch (Exception e) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getDataSource", "Caught exception " + e + ", throw InternalLogException");
            throw new InternalLogException("Failed to locate DataSource, caught exception", e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getDataSource", nonTranDataSource);
        return nonTranDataSource;
    }
}
