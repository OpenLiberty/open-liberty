/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.security.internal;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Set;

import javax.resource.spi.work.SecurityContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.message.callback.CallerPrincipalCallback;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.WSSecurityHelper;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * This class handles the association and dissociation of the security
 * context with the thread of execution of the Work that is submitted
 * by the resource adapter to the work manager.
 * 
 * @author jroast
 * 
 */
public class SecWorkContextHandler {

    final static TraceComponent tc = Tr.register(SecWorkContextHandler.class, "WAS.j2c.security", "com.ibm.ws.jca.security.resources.J2CAMessages");

    private static SecWorkContextHandler _instance;

    private SecWorkContextHandler() {}

    public static SecWorkContextHandler getInstance() {
        if (_instance == null) {
            _instance = new SecWorkContextHandler();
        }
        return _instance;
    }

    /**
     * This method is called by the WorkProxy class to associate the in-flown
     * SecurityContext with the work manager thread that will perform the work.
     * Note that this should support the case of nested work submission.
     * 
     * @param credService The credentials service
     * @param securityService The security service
     * @param unauthSubjService The unauthenticated subject service
     * @param authService The authentication service
     * @param workCtx The WorkContext (SecurityContext) to associate
     * @param providerId The id of the associated provider
     * @return void
     * @throws WorkCompletedException if there is an error during association of the
     *             SecurityContext
     */
    public void associate(CredentialsService credService, WSSecurityService securityService, UnauthenticatedSubjectService unauthSubjService, AuthenticationService authService,
                          WorkContext workCtx, String providerId)
                    throws WorkCompletedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "associate", new Object[] { J2CSecurityHelper.objectId(workCtx), providerId });
        }
        // Check if application security is enabled and only in that case
        // do the security context association.
        if (WSSecurityHelper.isServerSecurityEnabled()) {
            TraceNLS nls = J2CSecurityHelper.getNLS();
            try {
                UserRegistry registry = securityService.getUserRegistry(null);
                final String appRealm = registry != null ? registry.getRealm() : null;
                // The code below extracts the security work context and invokes
                // setupSecurityContext on it passing in the callback handler and the
                // execution and server subjects. 
                SecurityContext sc = (SecurityContext) workCtx;
                final Subject executionSubject = new Subject();
                J2CSecurityCallbackHandler handler = new J2CSecurityCallbackHandler(executionSubject, appRealm, credService.getUnauthenticatedUserid()); // Change from twas - jms - Setting to null for now - //cm.getUnauthenticatedString());

                Subject serverSubject = null;
                sc.setupSecurityContext(handler, executionSubject, serverSubject);
                SubjectHelper subjectHelper = new SubjectHelper();
                WSCredential credential = subjectHelper.getWSCredential(executionSubject);
                // check if the Subject is already authenticated i.e it contains WebSphere credentials.
                if (credential != null) {
                    if (handler.getInvocations()[0] == Invocation.CALLERPRINCIPALCALLBACK || // Begin 673415
                        handler.getInvocations()[1] == Invocation.GROUPPRINCIPALCALLBACK ||
                        handler.getInvocations()[2] == Invocation.PASSWORDVALIDATIONCALLBACK) {

                        String message = nls.getString("AUTHENTICATED_SUBJECT_AND_CALLBACK_NOT_SUPPORTED_J2CA0677",
                                                       "J2CA0677E: " +
                                                                       "An authenticated JAAS Subject and one or more JASPIC callbacks were passed to the application server " +
                                                                       "by the resource adapter."); // End 673415
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "associate");
                        }
                        throw new WSSecurityException(message);
                    } else if (appRealm.equals(credential.getRealmName()) || RegistryHelper.isRealmInboundTrusted(credential.getRealmName(), appRealm)) { // Begin 673415 
                        J2CSecurityHelper.setRunAsSubject(executionSubject);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "associate");
                        }
                        return;
                    } else {
                        String message = nls.getFormattedMessage("REALM_IS_NOT_TRUSTED_J2CA0685", new Object[] { null }, "REALM_IS_NOT_TRUSTED_J2CA0685"); // Change from twas - jms - TODO check on this - credential.getRealmName() changed to null
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "associate");
                        }
                        throw new WSSecurityException(message);
                    }
                }
                // After the invocation of the setupSecurityContext the execution subject will be
                // populated with the result of handling the callbacks that are passed in to the
                // handle method of the callback handler. The result is a custom hashtable that can
                // be used for security to do a hashtable login. The resource adapter can also modify
                // the execution subject by adding a principal. The code below checks for the result and
                // if the result is null or empty after the invocation of the callbacks throws an Exception
                // See JCA 1.6 Spec 16.4.1.
                Hashtable<String, Object> cred = J2CSecurityHelper.getCustomCredentials(executionSubject, handler.getCacheKey());
                Set<Principal> principals = executionSubject.getPrincipals(); // 675546
                if (handler.getInvocations()[0] == Invocation.CALLERPRINCIPALCALLBACK) {
                    if (cred == null || !cred.containsKey(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME)) {
                        String message = nls.getString("CUSTOM_CREDENTIALS_MISSING_J2CA0668",
                                                       "J2CA0668E: The WorkManager was unable to "
                                                                       +
                                                                       "populate the execution subject with the caller principal or credentials necessary to establish the security "
                                                                       +
                                                                       "context for this Work instance.");
                        throw new WSSecurityException(message);
                    }
                } else if ((handler.getInvocations()[1] == Invocation.GROUPPRINCIPALCALLBACK // Begin 673415
                           || handler.getInvocations()[2] == Invocation.PASSWORDVALIDATIONCALLBACK)
                           && principals.size() != 1) { //675546
                    // If CallerPrincipalCallback was not provided but other callbacks were provided then do not 
                    // allow the security context to be setup. See next comment for the reason
                    String message = nls.getString("CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669",
                                                   "J2CA0669E: The resource adapter " +
                                                                   "did not provide a CallerPrincipalCallback, an execution subject containing a single principal, or an empty " +
                                                                   "execution subject.");
                    throw new WSSecurityException(message);

                } else { // End 673415
                    // As per the JCA 1.6 Spec(16.4.5) the CallerPrincipalCallback should always be called except if the
                    // Resource Adapter wants to establish the UNAUTHENTICATED Identity by passing in an empty
                    // subject or when it passes a single principal in the principal set of the subject in which 
                    // case we handle it like a CallerPrincipalCallback is passed with that principal.
                    if (principals.isEmpty()) {
                        CallerPrincipalCallback cpCallback = new CallerPrincipalCallback(executionSubject, (String) null);
                        handler.handle(new Callback[] { cpCallback });
                    } else if (principals.size() == 1) {
                        CallerPrincipalCallback cpCallback = new CallerPrincipalCallback(executionSubject, principals.iterator().next());
                        executionSubject.getPrincipals().clear(); // 673415
                        handler.handle(new Callback[] { cpCallback });
                    } else {
                        String message = nls.getString("CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669",
                                                       "J2CA0669E: The resource adapter "
                                                                       +
                                                                       "did not provide a CallerPrincipalCallback, an execution subject containing a single principal, or an empty "
                                                                       +
                                                                       "execution subject.");
                        throw new WSSecurityException(message);
                    }
                    // Refresh the cred variable since we are calling the handler here.
                    cred = J2CSecurityHelper.getCustomCredentials(executionSubject, handler.getCacheKey());
                }
                // Do a custom hashtable login to get the runAs subject and set it on a ThreadLocal
                // so that we can retrieve it later in the WorkProxy to establish as the RunAs and Caller
                // subject.
                final String userName = (String) cred.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
                Subject runAsSubject = null;
                if (userName.equals(credService.getUnauthenticatedUserid())) {
                    runAsSubject = unauthSubjService.getUnauthenticatedSubject();
                } else {
                    final AuthenticationService authServ = authService;
                    PrivilegedExceptionAction<Subject> loginAction = new PrivilegedExceptionAction<Subject>() {
                        @Override
                        public Subject run() throws Exception {
                            return authServ.authenticate(JaasLoginConfigConstants.SYSTEM_DEFAULT, executionSubject);
                        }
                    };
                    runAsSubject = AccessController.doPrivileged(loginAction);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The RunAs subject is created after a successful login.");
                }
                J2CSecurityHelper.setRunAsSubject(runAsSubject);
            } catch (RuntimeException e) {
                Tr.error(tc, "SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671", e);
                String message = nls.getString("SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671",
                                               "J2CA0671E: The WorkManager was unable to associate the inflown SecurityContext to the Work instance.");
                WorkCompletedException workCompEx = new WorkCompletedException(
                                message, WorkException.INTERNAL);
                workCompEx.initCause(e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "associate");
                }
                throw workCompEx;
            } catch (Exception e) {
                Tr.error(tc, "SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671", e);
                String message = nls.getString("SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671",
                                               "J2CA0671E: The WorkManager was unable to associate the inflown SecurityContext to the Work instance.");
                WorkCompletedException workCompEx = new WorkCompletedException(
                                message, WorkException.INTERNAL);
                workCompEx.initCause(e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "associate");
                }
                throw workCompEx;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "associate");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "associate", "Application security is not enabled for the application server.");
            }
        }
    }

    /**
     * This method is called by the WorkProxy class to dissociate the inflown
     * SecurityContext from the workmanager thread after it has run the work
     * By the time this method is called, the WSSubject.doAs method call would
     * have exited and the runAs subject would be dissociated. So all that is
     * left to do here is to clear the ThreadLocal variable that is set with
     * the runAs subject in the associate method.
     * 
     * @return void
     */
    public void dissociate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "dissociate");
        }
        if (WSSecurityHelper.isServerSecurityEnabled()) {
            J2CSecurityHelper.removeRunAsSubject();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "dissociate");
        }
    }

}
