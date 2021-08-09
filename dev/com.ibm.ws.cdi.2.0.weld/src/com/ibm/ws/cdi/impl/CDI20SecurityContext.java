/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl;

import java.util.ArrayList;

import org.jboss.weld.security.spi.SecurityContext;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * The Liberty implementation of the
 * org.jboss.weld.security.spi.SecurityContext interface
 */
class CDI20SecurityContext implements SecurityContext {
    private ThreadContextDescriptor threadContextDesc = null;
    private ArrayList<ThreadContext> threadContext = null;

    CDI20SecurityContext(ThreadContextDescriptor tcd) {
        threadContextDesc = tcd;
    }

    /** {@inheritDoc} */
    @Override
    public void associate() {
        if (threadContextDesc != null)
            threadContext = threadContextDesc.taskStarting();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        // noop
    }

    /** {@inheritDoc} */
    @Override
    public void dissociate() {
        if (threadContextDesc != null)
            threadContextDesc.taskStopping(threadContext);
    }

}