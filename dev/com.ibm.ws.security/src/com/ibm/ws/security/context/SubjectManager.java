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
package com.ibm.ws.security.context;

import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.internal.SubjectThreadContext;
import com.ibm.ws.security.mp.jwt.proxy.MpJwtHelper;

/**
 * The SubjectManager sets and gets caller/invocation subjects off the thread
 * and provides the ability to clear the subjects off the thread.
 */
public class SubjectManager {

    private static final ThreadLocal<SubjectThreadContext> threadLocal = new SecurityThreadLocal();
    private static final SubjectHelper subjectHelper = new SubjectHelper();
    private static final TraceComponent tc = Tr.register(SubjectManager.class);

    /**
     * Sets the caller subject on the thread.
     */
    public void setCallerSubject(Subject callerSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        subjectThreadContext.setCallerSubject(callerSubject);
    }

    /**
     * Gets the caller subject from the thread.
     */
    public Subject getCallerSubject() {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        return subjectThreadContext.getCallerSubject();
    }

    /**
     * Sets the invocation subject on the thread.
     */
    public void setInvocationSubject(Subject invocationSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        subjectThreadContext.setInvocationSubject(invocationSubject);
    }

    /**
     * Gets the invocation subject from the thread.
     */
    public Subject getInvocationSubject() {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        return subjectThreadContext.getInvocationSubject();
    }

    /**
     * Replaces the caller subject on the thread and returns the replaced subject.
     */
    public Subject replaceCallerSubject(Subject callerSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        Subject replacedCallerSubject = subjectThreadContext.getCallerSubject();
        subjectThreadContext.setCallerSubject(callerSubject);
        return replacedCallerSubject;
    }

    /**
     * Replaces the invocation subject on the thread and returns the replaced subject.
     */
    public Subject replaceInvocationSubject(Subject invocationSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        Subject replacedInvocationSubject = subjectThreadContext.getInvocationSubject();
        subjectThreadContext.setInvocationSubject(invocationSubject);
        return replacedInvocationSubject;
    }

    /**
     * Clears the caller and invocation subjects by setting them to null.
     */
    public void clearSubjects() {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        subjectThreadContext.clearSubjects();
    }

    /**
     * Gets the subject thread context that is unique per thread.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     *
     * @return the subject thread context.
     */
    @Trivial
    protected SubjectThreadContext getSubjectThreadContext() {
        ThreadLocal<SubjectThreadContext> currentThreadLocal = getThreadLocal();
        SubjectThreadContext subjectThreadContext = currentThreadLocal.get();
        if (subjectThreadContext == null) {
            subjectThreadContext = new SubjectThreadContext();
            currentThreadLocal.set(subjectThreadContext);
        }
        return subjectThreadContext;
    }

    /**
     * Gets the thread local object.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     *
     * @return the thread local object.
     */
    @Trivial
    private ThreadLocal<SubjectThreadContext> getThreadLocal() {
        return threadLocal;
    }

    private static final class SecurityThreadLocal extends ThreadLocal<SubjectThreadContext> {
        @Override
        protected SubjectThreadContext initialValue() {
            return new SubjectThreadContext();
        }
    }

    public static Principal getCallerPrincipal(Subject callerSubject) {
        if (callerSubject == null) {
            return null;
        }

        if (subjectHelper.isUnauthenticated(callerSubject)) {
            return null;
        }

        // Here is the order to get the callerPrincipal
        // 1) jsonWebToken in subject
        // 2) From JASPIC property
        // 3) From WSCredential.getSecurityName
        // 4) From WSPrincipal
        // 5) First Principal in Subject

        // 1) From jsonWebToken in subject
        Principal jsonWebToken = MpJwtHelper.getJsonWebTokenPricipal(callerSubject);
        if (jsonWebToken != null) {
            return jsonWebToken;
        }

        WSCredential wscredential = subjectHelper.getWSCredential(callerSubject);

        // 2) From JASPIC property
        if (wscredential != null) {
            Principal principal = null;
            try {
                principal = (Principal) wscredential.get("com.ibm.wsspi.security.cred.jaspi.principal");
                if (principal != null)
                    return principal;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Internal error getting JASPIC Principal from credential", e);
                }
            }

            String securityName = null;
            // 3) From WSCredential.getSecurityName
            try {
                securityName = wscredential.getSecurityName();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error getting securityName from WSCredential", e);
                }
            }

            WSPrincipal wsPrincipal = null;
            if (securityName != null) {
                Set<WSPrincipal> principals = callerSubject.getPrincipals(WSPrincipal.class);
                if (!principals.isEmpty()) {
                    wsPrincipal = principals.iterator().next();
                    wsPrincipal = new WSPrincipal(securityName, wsPrincipal.getAccessId(), wsPrincipal.getAuthenticationMethod());
                }

                if (wsPrincipal != null) {
                    return wsPrincipal;
                }
            }

            // 4) From WSPrincipal
            Set<Principal> principals = callerSubject.getPrincipals();
            if (principals.size() > 0) {
                for (Iterator<Principal> iterator = principals.iterator(); iterator.hasNext();) {
                    principal = iterator.next();
                    if (principal instanceof WSPrincipal)
                        return principal;
                }
            }

            // There is no WSPrincipal so just return first one
            // 5) First Principal in Subject
            return principals.iterator().next();
        }

        return null;
    }
}
