/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.File;
import java.util.Map;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.ffdc.FFDCConfigurator;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.LogProvider;

/**
 *
 */
public class LogProviderImpl implements LogProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Map<String, String> config, File logLocation, TextFileOutputStreamFactory factory) {
        LogProviderConfigImpl loggingConfig = new LogProviderConfigImpl(config, logLocation, factory);

        TrConfigurator.init(loggingConfig);
        FFDCConfigurator.init(loggingConfig);
    }

    @Override
    public void stop() {
        // FFDC uses Tr, it must be stopped first
        FFDCConfigurator.stop();
        TrConfigurator.stop();
    }
}
