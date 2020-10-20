/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.util;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.transaction.NotSupportedException;

public class TMHelper {
    private static final String TMHelperClass = "com.ibm.tx.jta.embeddable.impl.EmbeddableTMHelper";

    private static TMService s;

    public static void setTMService(TMService tms) {
        s = tms; // check s not null before/after?
    }

    public static Object runAsSystem(PrivilegedExceptionAction a) throws PrivilegedActionException {
        return s.runAsSystem(a);
    }

    public static Object runAsSystemOrSpecified(PrivilegedExceptionAction a) throws PrivilegedActionException {
        return s.runAsSystemOrSpecified(a);
    }

    public static boolean isProviderInstalled(String providerId) {
        return s.isProviderInstalled(providerId);
    }

    public static void asynchRecoveryProcessingComplete(Throwable t) {
        s.asynchRecoveryProcessingComplete(t);
    }

    public static void start() throws Exception {
        if (s == null) {
            try {
                s = (TMService) Class.forName(TMHelperClass).newInstance();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        s.start();
    }

    public static void start(boolean waitForRecovery) throws Exception {
        if (s == null) {
            try {
                s = (TMService) Class.forName(TMHelperClass).newInstance();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        s.start(waitForRecovery);
    }

    public static void shutdown() throws Exception {
        s.shutdown();
    }

    public static void shutdown(int timeout) throws Exception {
        s.shutdown(timeout);
    }

    public static void checkTMState() throws NotSupportedException {
        if (s == null) {
            try {
                s = (TMService) Class.forName(TMHelperClass).newInstance();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        s.checkTMState();
    }

    public static void start(Map<String, Object> properties) throws Exception {
        start(); // For now
    }
}