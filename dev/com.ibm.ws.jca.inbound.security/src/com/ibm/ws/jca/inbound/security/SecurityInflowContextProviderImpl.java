/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
import java.util.List;
import java.util.Map;

import javax.resource.spi.work.SecurityContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;
import com.ibm.wsspi.threadcontext.jca.JCAContextProvider;

/**
 * Transaction context service provider.
 */
public class SecurityInflowContextProviderImpl implements JCAContextProvider, ThreadContextProvider {

    /**
     * Reference to the security service.
     */
    final AtomicServiceReference<Object> securityServiceRef = new AtomicServiceReference<Object>("securityService");

    /**
     * Reference to the UnauthenticatedSubject service.
     */
    final AtomicServiceReference<Object> unauthSubjectServiceRef = new AtomicServiceReference<Object>("unauthenticatedSubjectService");

    /**
     * Reference to the Authentication service.
     */
    final AtomicServiceReference<Object> authServiceRef = new AtomicServiceReference<Object>("authenticationService");

    /**
     * Reference to the Credentials service.
     */
    final AtomicServiceReference<Object> credServiceRef = new AtomicServiceReference<Object>("credentialsService");

    /**
     * Called during service activation.
     * 
     * @param context
     */
    protected void activate(ComponentContext context) {
        securityServiceRef.activate(context);
        unauthSubjectServiceRef.activate(context);
        authServiceRef.activate(context);
        credServiceRef.activate(context);
    }

    /**
     * Called during service deactivation.
     * 
     * @param context
     */
    protected void deactivate(ComponentContext context) {
        securityServiceRef.deactivate(context);
        unauthSubjectServiceRef.deactivate(context);
        authServiceRef.deactivate(context);
        credServiceRef.deactivate(context);

    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext getInflowContext(Object workContext, Map<String, String> execProps) {
        CredentialsService credService = (CredentialsService) credServiceRef.getServiceWithException();
        WSSecurityService wss = (WSSecurityService) securityServiceRef.getServiceWithException();
        UnauthenticatedSubjectService unauthSubjService = (UnauthenticatedSubjectService) unauthSubjectServiceRef.getServiceWithException();
        AuthenticationService authService = (AuthenticationService) authServiceRef.getServiceWithException();
        return new SecurityInflowContext(credService, wss, unauthSubjService, authService, workContext, execProps.get(WSContextService.TASK_OWNER));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#captureThreadContext(java.util.Map, java.util.Map)
     */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        throw new UnsupportedOperationException("captureThreadContext");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#deserializeThreadContext(java.util.Map, byte[])
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        throw new NotSerializableException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getDefaultThreadContext(java.util.Map)
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        String identityName = execProps.get(WSContextService.TASK_OWNER);
        SecurityContext defaultCtx = new SecurityContext() {
            private static final long serialVersionUID = 1924323423421234352L;

            @Override
            public void setupSecurityContext(CallbackHandler arg0, Subject arg1, Subject arg2) {
                // do nothing and instead use the default subjects passed in by the application server.
            }
        };
        CredentialsService credService = (CredentialsService) credServiceRef.getServiceWithException();
        WSSecurityService wss = (WSSecurityService) securityServiceRef.getServiceWithException();
        UnauthenticatedSubjectService unauthSubjService = (UnauthenticatedSubjectService) unauthSubjectServiceRef.getServiceWithException();
        AuthenticationService authService = (AuthenticationService) authServiceRef.getServiceWithException();
        return new SecurityInflowContext(credService, wss, unauthSubjService, authService, defaultCtx, identityName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    /**
     * Declarative Services method for setting the AuthenticationService service
     * 
     * @param ref reference to the service
     */
    protected void setAuthenticationService(ServiceReference<Object> ref) {
        authServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the AuthenticationService service
     * 
     * @param ref reference to the service
     */
    protected void unsetAuthenticationService(ServiceReference<Object> ref) {
        authServiceRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the UnauthenticatedSubjectService service
     * 
     * @param ref reference to the service
     */
    protected void setUnauthenticatedSubjectService(ServiceReference<Object> ref) {
        unauthSubjectServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the UnauthenticatedSubjectService service
     * 
     * @param ref reference to the service
     */
    protected void unsetUnauthenticatedSubjectService(ServiceReference<Object> ref) {
        unauthSubjectServiceRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the SecurityService service
     * 
     * @param ref reference to the service
     */
    protected void setSecurityService(ServiceReference<Object> ref) {
        securityServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the SecurityService service
     * 
     * @param ref reference to the service
     */
    protected void unsetSecurityService(ServiceReference<Object> ref) {
        securityServiceRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the CredentialsService service
     * 
     * @param ref reference to the service
     */
    protected void setCredentialsService(ServiceReference<Object> ref) {
        credServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the CredentialsService service
     * 
     * @param ref reference to the service
     */
    protected void unsetCredentialsService(ServiceReference<Object> ref) {
        credServiceRef.unsetReference(ref);
    }

}
