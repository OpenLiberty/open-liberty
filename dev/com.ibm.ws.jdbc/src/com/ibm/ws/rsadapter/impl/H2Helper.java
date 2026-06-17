/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.rsadapter.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Collections;

import javax.resource.ResourceException;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jdbc.internal.PropertyService;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Helper for the H2 JDBC driver.
 * Integrates H2 database driver logs with Liberty's logging infrastructure.
 */
public class H2Helper extends DatabaseHelper {
    private static final TraceComponent tc = Tr.register(H2Helper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    @SuppressWarnings("deprecation")
    private static final com.ibm.ejs.ras.TraceComponent h2Tc = 
        com.ibm.ejs.ras.Tr.register("com.ibm.ws.h2.logwriter", "WAS.database", null);

    /**
     * Construct a helper class for H2.
     * 
     * @param mcf managed connection factory
     */
    H2Helper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);

        // Add H2-specific stale connection error codes
        // These supplement the standard X/OPEN SQLSTATE codes inherited from DatabaseHelper:
        // - 08001 (unable to establish connection)
        // - 08003 (connection does not exist)
        // - 08006 (connection failure)
        // - 08S01 (communication link failure)
        Collections.addAll(staleConCodes,
            8000,   // ERROR_OPENING_DATABASE_1 - Problem trying to create a database lock.
            90007,  // OBJECT_CLOSED - JDBC object (connection/statement) has been closed
            90020,  // DATABASE_ALREADY_OPEN_1 - Database already open (embedded mode conflict)
            90067,  // CONNECTION_BROKEN_1 - Client connection lost or broken
            90098,  // DATABASE_IS_CLOSED - Database has been closed (out of memory, shutdown)
            90121); // DATABASE_CALLED_AT_SHUTDOWN - Database accessed during shutdown
    }

    /**
     * Returns a PrintWriter that wraps Liberty's TraceWriter for H2 driver logging.
     * This enables H2 driver logs to be integrated with Liberty's trace infrastructure.
     * 
     * @return PrintWriter for H2 driver logging
     * @throws ResourceException if an error occurs creating the PrintWriter
     */
    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        if (genPw == null)
            genPw = new PrintWriter(new FilteringTraceWriter(h2Tc), true);
        return genPw;
    }

    /**
     * Returns the trace component for H2 driver logging.
     * 
     * @return the trace component for H2 supplemental trace
     */
    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return h2Tc;
    }

    /**
     * Determines if H2 driver trace should be enabled.
     * Trace is enabled when Liberty tracing is enabled, the H2 trace component
     * is set to debug level, and logging is not already enabled.
     * 
     * @param mcf the managed connection factory
     * @return true if trace should be enabled, false otherwise
     */
    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        return TraceComponent.isAnyTracingEnabled() && 
               h2Tc.isDebugEnabled() && 
               !mcf.loggingEnabled;
    }

    /**
     * Determines if H2 driver trace should be disabled.
     * Trace is disabled when Liberty tracing is enabled, the H2 trace component
     * is not set to debug level, and logging is currently enabled.
     * 
     * @param mc the managed connection
     * @return true if trace should be disabled, false otherwise
     */
    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        return TraceComponent.isAnyTracingEnabled() &&
               !h2Tc.isDebugEnabled() &&
               mc.mcf.loggingEnabled;
    }

    /**
     * Extension of TraceWriter that filters sensitive information from H2 driver logs.
     * Filters all lines starting with "Content: " to prevent sensitive data exposure.
     */
    private static class FilteringTraceWriter extends TraceWriter {
        private static final String CONTENT_PREFIX = "Content: ";
        private static final String FILTERED_VALUE = "******";

        private final com.ibm.ejs.ras.TraceComponent traceDestination;

        public FilteringTraceWriter(com.ibm.ejs.ras.TraceComponent dest) {
            super(dest);
            this.traceDestination = dest;
        }

        @Override
        protected void formatTrace() {
            final String str = toString();
            int start = 0;

            for (int end = str.indexOf('\n'); end >= 0; start = end + 1, end = str.indexOf('\n', start)) {
                String line = str.substring(start, end);
                com.ibm.websphere.ras.Tr.debug(traceDestination, filterLine(line));
            }

            getBuffer().delete(0, start);
        }

        /**
         * Filters all lines starting with "Content: " to mask sensitive information.
         */
        private String filterLine(String line) {
            String trimmed = line.trim();

            if (trimmed.startsWith(CONTENT_PREFIX)) {
                return withOriginalIndentation(line, CONTENT_PREFIX + FILTERED_VALUE);
            }

            return line;
        }

        private String withOriginalIndentation(String line, String replacement) {
            int prefixIndex = line.indexOf(CONTENT_PREFIX);
            return prefixIndex > 0 ? line.substring(0, prefixIndex) + replacement : replacement;
        }
    }
}