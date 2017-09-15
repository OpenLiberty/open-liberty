/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.inbound.security;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.jca.security.JCASecurityContext;
import com.ibm.ws.security.context.SubjectManager;

/**
 * Runs a work instance under the invocation subject present on the thread of execution.
 */
public class JCASecurityContextService implements JCASecurityContext {

    private final SubjectManager subjectManager = new SubjectManager();

    /** {@inheritDoc} */
    @Override
    public void runInInboundSecurityContext(final Runnable work) {
        Subject doAsSubject = subjectManager.getInvocationSubject();
        PrivilegedAction<Runnable> privEx = new PrivilegedAction<Runnable>() {
            @Override
            public Runnable run() {
                work.run();
                return null;
            }
        };
        if (doAsSubject != null) {
            WSSubject.doAs(subjectManager.getInvocationSubject(), privEx);
        } else {
            work.run();
        }
    }

}
