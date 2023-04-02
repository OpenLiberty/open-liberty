/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.logging.internal.impl;

import java.io.IOException;
import java.util.logging.LogRecord;

import com.ibm.ws.kernel.boot.logging.WsLogManager;
import com.ibm.ws.logging.utils.FileLogHolder;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 *
 */
public class Jsr47TraceService extends BaseTraceService {

    /** {@inheritDoc} */
    @Override
    public void init(LogProviderConfig config) {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            super.init(config);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void update(LogProviderConfig config) {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            super.init(config);
        }
    }

    @Override
    public void stop() {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            super.stop();
        }
    }

    @Override
    protected void registerLoggerHandlerSingleton() {
    }

    @Override
    protected void unregisterLoggerHandlerSingleton() {
    }

    /** {@inheritDoc} */
    @Override
    protected void initializeWriters(LogProviderConfigImpl config) {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            // createFileLog may or may not return the original log holder..
            if (config.isRestore()) {
                messagesLog = null;
            }
            messagesLog = FileLogHolder.createFileLogHolder(messagesLog,
                                                            null,
                                                            config.getLogDirectory(),
                                                            config.getMessageFileName(),
                                                            config.getMaxFiles(),
                                                            config.getMaxFileBytes(),
                                                            config.getNewLogsOnStart(),
                                                            config.isRestore());

            // Always create a traceLog when using Tr -- this file won't actually be
            // created until something is logged to it...
            traceLog = new TraceWriter() {

                @Override
                public void writeRecord(String record) {
                }

                @Override
                public void close() throws IOException {
                }
            };
        }
    }

    /** {@inheritDoc} */
    @Override
    public void publishLogRecord(LogRecord logRecord) {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            super.publishLogRecord(logRecord);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void captureSystemStreams() {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            super.captureSystemStreams();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreSystemStreams() {
        if (!WsLogManager.isConfiguredByLoggingProperties()) {
            super.restoreSystemStreams();
        }
    }
}
