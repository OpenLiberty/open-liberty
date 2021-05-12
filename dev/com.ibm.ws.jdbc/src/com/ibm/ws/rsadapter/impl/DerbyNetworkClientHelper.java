/*******************************************************************************
 * Copyright (c) 2001, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivilegedAction;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import javax.resource.ResourceException;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;
import java.security.AccessController;

/**
 * Helper for the Derby Network Client JDBC driver.
 */
public class DerbyNetworkClientHelper extends DerbyHelper { 
    private static final Class<?> currClass = DerbyNetworkClientHelper.class; 
    private static transient TraceComponent tc = Tr.register(currClass, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE); 

    private static final String
        TRACE_FILE = "traceFile",
        TRACE_FILE_DIR = "traceDirectory",
        TRACE_FILE_APPEND = "traceFileAppend";

    private transient String traceFile; 	
    boolean traceAppend = false; 
    String traceDir = null; 

    /**
     * Construct a helper class for the Derby Network Client JDBC driver.
     *  
     * @param mcf managed connection factory
     */
    DerbyNetworkClientHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        dataStoreHelperClassName = "com.ibm.websphere.rsadapter.DerbyNetworkServerDataStoreHelper";

        mcf.doesStatementCacheIsoLevel = true;

        Properties props = mcf.dsConfig.get().vendorProps;
        // we are not reading the tracelevel since we don't set the trace on the connection as we do with dB2, 
        // with Derby, we set the trace on the DS level only.  NO API from Derby to set per connection.
        // now get the value of the trace file 
        traceFile = props.getProperty(TRACE_FILE);
        //using the same property as the one for DB2, since really its using db2 universal

        // ============= now read the value for traceDir
        traceDir = props.getProperty(TRACE_FILE_DIR);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "traceDir is set to ", traceDir);
        }

        if (traceDir != null && !traceDir.equals("")) {
            traceDir = traceDir + File.separator;
        }
        else {
            traceDir = "";
        }

        //============= now read the value for traceAppend
        Object holder = props.get(TRACE_FILE_APPEND); 
        traceAppend = holder instanceof Boolean ? (Boolean) holder
                    : holder instanceof String ? Boolean.valueOf((String) holder)
                    : false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Trace Append is set to ", holder); // printing holder here since we need to see the value that got set, 
            // not what we ended up picking.
        }

        // construct the printWriter for derby network server
        if ((traceFile != null) && (!traceFile.equals(""))) {
            // traceFile is set
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Derby network server JDBC trace was configured to go to a file, Thus no integration with WAS trace.  File name is: ", traceDir + traceFile); 

            genPw = AccessController.doPrivileged(new PrivilegedAction<PrintWriter>() {
                public PrintWriter run() {
                    try {
                        return new PrintWriter(new FileOutputStream(traceDir + traceFile, traceAppend), true); 
                    } catch (IOException e) {
                        // if i get an io exception, log a warning and use integrated logging instead
                        Tr.error(tc, "DB2_FILE_OUTSTREAM_ERROR", traceFile);
                        // will use the same message since its using DB2 universal driver
                        return null;
                    }
                }
            });
        }
        else { // means need to integrate
            genPw = new PrintWriter(new TraceWriter(derbyTc), true);
        }
        
        Collections.addAll(staleConCodes,
                           -4499);
    }

    @Override
    public void doStatementCleanup(PreparedStatement stmt) throws SQLException {
        if (dataStoreHelper != null) {
            doStatementCleanupLegacy(stmt);
            return;
        }

        // setCursorName not supported in network server
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        stmt.setMaxFieldSize(0);
        stmt.setMaxRows(0);

        Integer queryTimeout = mcf.dsConfig.get().queryTimeout;
        if (queryTimeout == null)
            queryTimeout = defaultQueryTimeout;
        stmt.setQueryTimeout(queryTimeout);
    }

    /**
     * @return NULL because Derby Network Server provides sufficient trace of its own.
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return null;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        //not synchronizing here since there will be one helper
        // and most likely the setting will be serially, even if its not, 
        // it shouldn't matter here (tracing).

        // Order of printwriter lookup:
        // first the retruned value from the externalhelper.getPrintWriter
        // then, based on the tracewriter.

        if (genPw == null) {
            genPw = new java.io.PrintWriter(new TraceWriter(derbyTc), true);
        }
        Tr.debug(derbyTc, "returning", genPw);
        return genPw;
    }

    @Override
    public void enableJdbcLogging(WSManagedConnectionFactoryImpl mcf) throws ResourceException {
        PrintWriter pw = getPrintWriter();
        if (TraceComponent.isAnyTracingEnabled() && derbyTc.isDebugEnabled())
            Tr.debug(derbyTc, "enabling Derby logging {mcf, pw}: ", new Object[] { mcf, pw });
        mcf.reallySetLogWriter(pw);
        mcf.loggingEnabled = true;
    }

}
