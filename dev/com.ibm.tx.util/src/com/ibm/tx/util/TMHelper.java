/*******************************************************************************
 * Copyright (c) 2009, 2024 IBM Corporation and others.
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
package com.ibm.tx.util;

import java.util.Map;

import javax.transaction.NotSupportedException;

public class TMHelper {
    private static final String TMHelperClass = "com.ibm.tx.jta.embeddable.impl.EmbeddableTMHelper";

    private static TMService s;

    public static void setTMService(TMService tms) {
        s = tms;
    }

    public static boolean isProviderInstalled(String providerId) {
        return s.isProviderInstalled(providerId);
    }

    public static void asynchRecoveryProcessingComplete(Throwable t) {
        s.asynchRecoveryProcessingComplete(t);
    }

    public static void start() throws Exception {
        if (s == null)
            s = TestTMHelperHolder.TM_SERVICE;
        s.start();
    }

    public static void start(boolean waitForRecovery) throws Exception {
        if (s == null)
            s = TestTMHelperHolder.TM_SERVICE;
        s.start(waitForRecovery);
    }

    public static void shutdown() throws Exception {
        s.shutdown();
    }

    public static void shutdown(int timeout) throws Exception {
        s.shutdown(timeout);
    }

    public static void checkTMState() throws NotSupportedException {
        if (s == null)
            s = TestTMHelperHolder.TM_SERVICE;
        s.checkTMState();
    }

    public static void start(Map<String, Object> properties) throws Exception {
        start();
    }

    /** For unit testing outside OSGi. */
    private enum TestTMHelperHolder {
        ;
        private static final TMService TM_SERVICE;
        static {
            try {
                @SuppressWarnings("unchecked")
                Class<TMService> HELPER_CLASS = (Class<TMService>) Class.forName(TMHelperClass);
                TM_SERVICE = HELPER_CLASS.getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                throw new Error(e);
            }
        }
    }
}