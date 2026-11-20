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
package com.ibm.ws.jdbc.internal;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Issues one-time notices for databases that are
 * no longer community maintained (Derby) or intended for development only (H2).
 */
public class DatabaseNotices {
    private static final TraceComponent tc = Tr.register(DatabaseNotices.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    private static final String PID_PREFIX = "com.ibm.ws.jdbc.dataSource.properties.";

    private static final AtomicBoolean derbyNoticeIssued = new AtomicBoolean();
    private static final AtomicBoolean h2NoticeIssued = new AtomicBoolean();

    /**
     * Issues the Derby warning and/or H2 informational message, each at most once per JVM.
     *
     * @param vendorPID        vendor properties PID, such as com.ibm.ws.jdbc.dataSource.properties.derby.embedded. Can be null.
     * @param classNames       configured or inferred data source/driver class names. Can be null.
     */
    public static void issueNotices(String vendorPID, Collection<String> classNames) {
        if (isDerby(vendorPID, classNames) && derbyNoticeIssued.compareAndSet(false, true)) {
            Tr.warning(tc, "8071_DERBY_NOT_MAINTAINED");
        }

        if (isH2(vendorPID, classNames) && h2NoticeIssued.compareAndSet(false, true)) {
            Tr.info(tc, "8072_H2_DEVELOPMENT_ONLY");
        }
    }

    static boolean isDerby(String vendorPID, Collection<String> classNames) {
        if (vendorPID != null && (vendorPID.startsWith(PID_PREFIX + "derby.embedded") || vendorPID.startsWith(PID_PREFIX + "derby.client")))
            return true;
        if (classNames != null)
            for (String className : classNames)
                if (className != null && className.startsWith("org.apache.derby.jdbc."))
                    return true;
        return false;
    }

    static boolean isH2(String vendorPID, Collection<String> classNames) {
        if (vendorPID != null && vendorPID.startsWith(PID_PREFIX + "h2"))
            return true;
        if (classNames != null)
            for (String className : classNames)
                if (className != null && className.startsWith("org.h2."))
                    return true;
        return false;
    }
}
