/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * This class is provided to overcome a Declarative Services bug where the DataSource may not
 * be fully ready at the time that the transaction component has been notified that the Service
 * is available.
 * 
 */
public class DeferredRecoveryStarter implements Runnable {

    private static final TraceComponent tc = Tr.register(DeferredRecoveryStarter.class);
    private ConfigurationProvider _cp = null;
    private ResourceFactory _dataSourceFactory = null;

    public DeferredRecoveryStarter(ConfigurationProvider cp, ResourceFactory dataSourceFactory)
    {
        _cp = cp;
        _dataSourceFactory = dataSourceFactory;
    }

    @FFDCIgnore(InterruptedException.class)
    @Override
    public void run()
    {
        boolean refSet = false;
        DataSource nonTranDataSource = null;

        while (!refSet)
        {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Sleep Interrupted");
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getDataSource after sleep");
            refSet = checkDataSource(nonTranDataSource);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Done sleeping drive startup");
        if (_cp.isRecoverOnStartup()) {
            try {
                TMHelper.start(_cp.isWaitForRecovery());
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.ws.transaction.services.TransactionManagerService.activate", "60", this);
            }
        }

    }

    /**
     * See whether it is possible to get a connection and metatdata from a DataSOurce.
     * 
     * @param nonTranDataSource
     * @return true if the DS is usable
     */
    @FFDCIgnore(Exception.class)
    private boolean checkDataSource(DataSource nonTranDataSource)
    {
        boolean fullyFormedDS = false;
        try
        {
            nonTranDataSource = (DataSource) _dataSourceFactory.createResource(null);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Non Tran dataSource is " + nonTranDataSource);
            Connection conn = nonTranDataSource.getConnection();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Established connection " + conn);
            DatabaseMetaData mdata = conn.getMetaData();
            String dbName = mdata.getDatabaseProductName();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Database name " + dbName);
            String dbVersion = mdata.getDatabaseProductVersion();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Database version " + dbVersion);
            fullyFormedDS = true;
        } catch (Exception e)
        {
            // We will catch an exception if the DataSource is not yet fully formed
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception: " + e);
        }

        return fullyFormedDS;
    }
}
