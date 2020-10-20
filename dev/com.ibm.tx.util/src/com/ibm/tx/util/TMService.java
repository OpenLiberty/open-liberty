/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import javax.transaction.NotSupportedException;

public interface TMService {
    public enum TMStates {
        INACTIVE, REPLAYING, RECOVERING, ACTIVE, STOPPING, STOPPED
    };

    public Object runAsSystem(PrivilegedExceptionAction a) throws PrivilegedActionException;

    public Object runAsSystemOrSpecified(PrivilegedExceptionAction a) throws PrivilegedActionException;

    public boolean isProviderInstalled(String providerId);

    public void asynchRecoveryProcessingComplete(Throwable t);

    public void start() throws Exception;

    public void start(boolean waitForRecovery) throws Exception;

    public void shutdown() throws Exception;

    public void shutdown(int timeout) throws Exception;

    public void checkTMState() throws NotSupportedException;
}