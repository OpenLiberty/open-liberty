/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.inbound.security;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.concurrent.RejectedExecutionException;

import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkContextErrorCodes;
import javax.resource.spi.work.WorkContextLifecycleListener;
import javax.security.auth.Subject;

import com.ibm.ws.jca.security.internal.J2CSecurityHelper;
import com.ibm.ws.jca.security.internal.SecWorkContextHandler;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Security inflow context.
 */
public class SecurityInflowContext implements ThreadContext {
    private static final long serialVersionUID = 4737189049209862723L;

    /**
     * Identifier for the resource adapter that provides the SecurityContext.
     */
    private final String resourceAdapterIdentifier;

    /**
     * The security context that is provided by the resource adapter.
     */
    private final WorkContext securityContext;

    /**
     * The SubjectManager.
     */
    private final transient SubjectManager subjectManager = new SubjectManager();

    /**
     * The WSSecurity Service.
     */
    private final transient WSSecurityService securityService;

    /**
     * The UnauthenticatedSubject Service.
     */
    private final transient UnauthenticatedSubjectService unauthSubjService;

    /**
     * The Authentication Service.
     */
    private final transient AuthenticationService authService;

    /**
     * The Credentials Service.
     */
    private final transient CredentialsService credService;

    /**
     * The caller subject prior to the establishment of this inflow context
     */
    private Subject priorCallerSubject;

    /**
     * The invocation subject prior to the establishment of this inflow context
     */
    private Subject priorInvocationSubject;

    /**
     * Constructs security inflow context.
     * 
     * @param securityContext SecurityContext.
     * @param resourceAdapterIdentifier identifier for the resource adapter.
     */
    public SecurityInflowContext(CredentialsService credService, WSSecurityService wss, UnauthenticatedSubjectService unauthSubjService,
                                 AuthenticationService authService, Object securityContext, String resourceAdapterIdentifier) {
        this.resourceAdapterIdentifier = resourceAdapterIdentifier;
        this.securityContext = (WorkContext) securityContext;
        this.securityService = wss;
        this.authService = authService;
        this.unauthSubjService = unauthSubjService;
        this.credService = credService;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            SecurityInflowContext copy = (SecurityInflowContext) super.clone();
            copy.priorCallerSubject = null;
            copy.priorInvocationSubject = null;
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContext#taskStarting()
     */
    @Override
    public void taskStarting() {
        try {
            SecWorkContextHandler.getInstance().associate(credService, securityService, unauthSubjService, authService, securityContext, resourceAdapterIdentifier);
            Subject subj = J2CSecurityHelper.getRunAsSubject();
            priorCallerSubject = subjectManager.getCallerSubject();
            priorInvocationSubject = subjectManager.getInvocationSubject();
            subjectManager.setCallerSubject(subj);
            subjectManager.setInvocationSubject(subj);
            if (securityContext instanceof WorkContextLifecycleListener)
                ((WorkContextLifecycleListener) securityContext).contextSetupComplete();
        } catch (WorkCompletedException x) {
            subjectManager.setCallerSubject(priorCallerSubject);
            subjectManager.setInvocationSubject(priorInvocationSubject);
            if (securityContext instanceof WorkContextLifecycleListener)
                ((WorkContextLifecycleListener) securityContext).contextSetupFailed(WorkContextErrorCodes.CONTEXT_SETUP_FAILED);
            throw new RejectedExecutionException(x);
        }

    }

    /***
     * @see com.ibm.wsspi.threadcontext.ThreadContext#taskStopping()
     */
    @Override
    public void taskStopping() {
        SecWorkContextHandler.getInstance().dissociate();
        subjectManager.setCallerSubject(priorCallerSubject);
        subjectManager.setInvocationSubject(priorInvocationSubject);
    }

    /**
     * Serialization is not supported for inflow context.
     * 
     * @param outStream The stream to write the serialized data.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream outStream) throws IOException {
        throw new NotSerializableException();
    }
}
