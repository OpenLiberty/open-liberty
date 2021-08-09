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

package com.ibm.ws.logging.internal;

import java.io.File;

import com.ibm.wsspi.logprovider.FFDCFilterService;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 * Disabled FFDCFilterService: all methods should be empty. If was.ffdc.enabled
 * is set to false via properties, this delegate will be used instead of the
 * BasicFFDCService, disabling all FFDC output.
 */
public class DisabledFFDCService implements FFDCFilterService {
    /**
     * @see com.ibm.wsspi.logprovider.FFDCFilterService#processException(java.lang.Throwable, java.lang.String, java.lang.String)
     */
    public void processException(Throwable th, String sourceId, String probeId) {}

    /**
     * @see com.ibm.wsspi.logprovider.FFDCFilterService#processException(java.lang.Throwable, java.lang.String, java.lang.String, java.lang.Object)
     */
    public void processException(Throwable th, String sourceId, String probeId, Object callerThis) {}

    /**
     * @see com.ibm.wsspi.logprovider.FFDCFilterService#processException(java.lang.Throwable, java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void processException(Throwable th, String sourceId, String probeId, Object[] objectArray) {}

    /**
     * @see com.ibm.wsspi.logprovider.FFDCFilterService#processException(java.lang.Throwable, java.lang.String, java.lang.String, java.lang.Object, java.lang.Object[])
     */
    public void processException(Throwable th, String sourceId, String probeId, Object callerThis, Object[] objectArray) {}

    @Override
    public void stop() {}

    @Override
    public void init(LogProviderConfig config) {}

    @Override
    public void update(LogProviderConfig config) {}

    @Override
    public void rollLogs() {}

    @Override
    public File getFFDCLogLocation() {
        return new File(".");
    }
}
