/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.security.intfc.internal;

import javax.security.auth.Subject;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.intfc.SubjectManagerService;

/**
 *
 */
public class SubjectManagerServiceImpl implements SubjectManagerService {

    protected void activate(ComponentContext cc) {}

    protected void deactivate(ComponentContext cc) {}

    /** {@inheritDoc} */
    @Override
    public void setCallerSubject(Subject callerSubject) {
        SubjectManager sm = new SubjectManager();
        sm.setCallerSubject(callerSubject);
    }

    /** {@inheritDoc} */
    @Override
    public Subject getCallerSubject() {
        SubjectManager sm = new SubjectManager();
        return sm.getCallerSubject();
    }

    /** {@inheritDoc} */
    @Override
    public void setInvocationSubject(Subject invocationSubject) {
        SubjectManager sm = new SubjectManager();
        sm.setInvocationSubject(invocationSubject);
    }

    /** {@inheritDoc} */
    @Override
    public Subject getInvocationSubject() {
        SubjectManager sm = new SubjectManager();
        return sm.getInvocationSubject();
    }

}
