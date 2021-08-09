/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.io.File;
import java.io.IOException;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This holds the location where recovery logs are kept
 */
public class LeaseInfo {
    private static final TraceComponent tc = Tr.register(LeaseInfo.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private File leaseDetail;

    /**
     * @return the leaseDetail
     */
    public File getLeaseDetail() {
        if (tc.isDebugEnabled())
            try {
                Tr.debug(tc, "getLeaseDetail", leaseDetail.getCanonicalPath());
            } catch (IOException e) {
                Tr.debug(tc, "getLeaseDetail", leaseDetail.getAbsolutePath());
            }
        return leaseDetail;
    }

    /**
     * @param leaseDetail the leaseDetail to set
     */
    public void setLeaseDetail(File leaseDetail) {
        if (tc.isDebugEnabled())
            try {
                Tr.debug(tc, "setLeaseDetail", leaseDetail.getCanonicalPath());
            } catch (IOException e) {
                Tr.debug(tc, "setLeaseDetail", leaseDetail.getAbsolutePath());
            }
        this.leaseDetail = leaseDetail;
    }

}
