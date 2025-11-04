/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.security.internal;

import javax.security.auth.Subject;

import com.ibm.ejs.container.BeanMetaData;

public class EJBSecurityContext {

    private Subject invokedSubject = null;
    private Subject receivedSubject = null;

    /**
     * This is the token returned by a request to sync the app's invocation subject
     * identity to the thread, via ThreadIdentityManager.set(). It represents the
     * previously sync'ed identity, and must be passed back to ThreadIdentityManager.reset(token),
     * to restore the previously sync'ed identity.
     */
    private Object syncToOSThreadToken = null;

    public EJBSecurityContext(Subject iSubject, Subject rSubject) {
        if (BeanMetaData.isRunningBetaMode()) {
            invokedSubject = iSubject;
            receivedSubject = rSubject;
        }
    }

    public Subject getInvokedSubject() {
        if (BeanMetaData.isRunningBetaMode())
            return invokedSubject;
        else
            return null;
    }

    public Subject getReceivedSubject() {
        if (BeanMetaData.isRunningBetaMode())
            return receivedSubject;
        else
            return null;
    }

    public void setSyncToOSThreadToken(Object obj) {
        if (BeanMetaData.isRunningBetaMode())
            syncToOSThreadToken = obj;
    }

    public Object getSyncToOSThreadToken() {
        if (BeanMetaData.isRunningBetaMode())
            return syncToOSThreadToken;
        else
            return null;
    }

}
