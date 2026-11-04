/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.thread.zos.context.internal;

import java.io.ObjectStreamField;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.ws.kernel.security.thread.ThreadIdentityException;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This class establishes the native thread identity for a unit of work
 * (aka SyncToOSThread). The runAs (invocation) subject's identity is sync'ed
 * to the native thread.
 *
 * An instance of this object is created by the work-queueing thread via
 * ThreadIdentityContextProviderImpl.captureThreadContext/createDefaultThreadContext.
 *
 * The thread that executes the work then calls taskStarting() and taskStopping()
 * on this object, to sync and un-sync the identity, respectively.
 */
public class ThreadIdentityContextImpl implements ThreadContext {
    private static final long serialVersionUID = -2446881258255227661L;

    /**
     * Fields to serialize. This is included for compatibility with future versions
     * in case we ever need to serialize additional fields.
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] { new ObjectStreamField("isDefaultContext", Boolean.TYPE) };

    /**
     * Token returned when sync'ing to the native thread identity.
     */
    private transient Object syncToken = null;

    /**
     * If true, the server's ID is sync'ed to the thread. Otherwise the
     * JEE runAs ID is sync'ed.
     */
    private boolean isDefaultContext = false;

    /**
     * @param isDefaultContext set the object's isDefaultContext field.
     */
    public ThreadIdentityContextImpl setIsDefaultContext(boolean isDefaultContext) {
        this.isDefaultContext = isDefaultContext;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            ThreadIdentityContextImpl copy = (ThreadIdentityContextImpl) super.clone();
            copy.syncToken = null;
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        try {
            syncToken = syncToOSThread();
        } catch (ThreadIdentityException e) {
            throw new RejectedExecutionException(e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void taskStopping() {
        resetSyncToOSThread(syncToken);
        syncToken = null;
    }

    /**
     * @return a reference to the SubjectManager, for retrieving the runAs subject on the thread.
     */
    protected SubjectManager getSubjectManager() {
        return new SubjectManager();
    }

    /**
     * Sync the invocation Subject's identity to the thread, if the application
     * component and the server both have thread identity enabled.
     *
     * @return the syncToken to provide on reset()
     * @throws ThreadIdentityException
     */
    protected Object syncToOSThread() throws ThreadIdentityException {
        return (isDefaultContext) ? ThreadIdentityManager.runAsServer() : ThreadIdentityManager.setAppThreadIdentity(getSubjectManager().getInvocationSubject());
    }

    /**
     * Remove the invocation Subject's identity from the thread, if it was previously sync'ed.
     *
     * @param resetToken The token previously returned by set()
     */
    protected void resetSyncToOSThread(Object resetToken) {
        if (resetToken != null) {
            ThreadIdentityManager.reset(resetToken);
        }
    }

}
