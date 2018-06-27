/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import java.io.File;
import java.util.Map;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.ffdc.FFDCConfigurator;
import com.ibm.ws.kernel.boot.logging.WsLogManager;
import com.ibm.ws.logging.hpel.config.HpelConfigurator;
import com.ibm.ws.logging.internal.impl.LoggingConstants;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.LogProvider;

/**
 *
 */
public class HpelLogProviderImpl implements LogProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Map<String, String> config, File logLocation, TextFileOutputStreamFactory factory) {
        // Use HPEL as a TrService provider if it was not explicitly set.
        if (!config.containsKey(config.get(LoggingConstants.PROP_TRACE_DELEGATE))) {
            config.put(LoggingConstants.PROP_TRACE_DELEGATE, HpelBaseTraceService.class.getName());
        }

        // Set boolean to enable HPEL, for the WsLogManager
        WsLogManager.setBinaryLoggingEnabled(true);

        HpelTraceServiceConfig loggingConfig = new HpelTraceServiceConfig(config, logLocation, factory);

        TrConfigurator.init(loggingConfig);
        FFDCConfigurator.init(loggingConfig);
        HpelConfigurator.init(loggingConfig);
    }

    @Override
    public void stop() {
        // FFDC uses Tr, it must be stopped first
        FFDCConfigurator.stop();
        TrConfigurator.stop();
    }
}
