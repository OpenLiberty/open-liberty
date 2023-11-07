/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class LeaseLogImpl {
    private static final TraceComponent tc = Tr.register(LeaseLogImpl.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);
    private static String _backendURL;

    protected String getBackendURL() {
        if (_backendURL != null) {
            return _backendURL;
        }

        _backendURL = ConfigurationProviderManager.getConfigurationProvider().getBackendURL();

        if (_backendURL == null || _backendURL.isEmpty()) {
            _backendURL = "http://localhost:9080";
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "backendURL: {0}", _backendURL);
        return _backendURL;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#lockPeerLease(java.lang.String)
     */
    public boolean lockPeerLease(String recoveryIdentity) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releasePeerLease(java.lang.String)
     */
    public void releasePeerLease(String recoveryIdentity) throws Exception {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#lockLocalLease(java.lang.String)
     */
    public boolean lockLocalLease(String recoveryIdentity) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.SharedServerLeaseLog#releaseLocalLease(java.lang.String)
     */
    public void releaseLocalLease(String recoveryIdentity) throws Exception {
    }
}