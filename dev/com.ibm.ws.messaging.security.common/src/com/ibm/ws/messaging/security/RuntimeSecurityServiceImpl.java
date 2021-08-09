/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.security;

import javax.security.auth.Subject;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationException;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
  * There is always exactly one RuntimeSecurityService
  * If MessagingSecurity is not defined then a default (always pass) authentication and authorization
  * service is used.
 *
 * @author Sharath Chandra B
 *
 */
@Component(configurationPid = "com.ibm.ws.messaging.security.RuntimeSecurityServiceImpl",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class RuntimeSecurityServiceImpl implements RuntimeSecurityService {

    // Trace component for the Authentication class
    private static TraceComponent tc = SibTr.register(RuntimeSecurityServiceImpl.class,
                                                      MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
                                                      MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

    // Absolute class name along with the package used for tracing
    private static final String CLASS_NAME = "com.ibm.ws.messaging.security.RuntimeSecurityService";

    // The active MessagingSecurityService or null if no MessagingSecurity is defined.
    private MessagingSecurityService messagingSecurityService = null;
    // Containers for the Authentication and Authorization services provided by the MessagingSecurityService.
    private final Authentication authentication = new Authentication(this);
    private final Authorization authorization = new Authorization(this);

    private Subject unauthenticatedSubject = null;

    public RuntimeSecurityServiceImpl() {
        final String methodName = "RuntimeSecurityServiceImpl";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, methodName, new Object[] {this});

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, methodName);
    }

    @Activate
    void activate() {
        final String methodName = "activate";
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, methodName, new Object[] {this, messagingSecurityService==null});
    }

    /**
     * Start using the MessagingSecurityService
     *
     * If the MessagingSecurityService is defined, prevent this service from starting until it is available.
     *
     * @param messagingSecurityService which has been defined.
     */
    //TODO Note that security is not ReferencePolicy.DYNAMIC, it cannot be created or destroyed the while messaging is started.
    @Reference(service = MessagingSecurityService.class, cardinality = ReferenceCardinality.OPTIONAL )
    protected void setMessagingSecurityService(MessagingSecurityService messagingSecurityService) {
        final String methodName = "setMessagingSecurityService";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, methodName, new Object[] {this, messagingSecurityService});

        this.messagingSecurityService = messagingSecurityService;
        authentication.setMessagingAuthenticationService(messagingSecurityService.getMessagingAuthenticationService());
        authorization.setMessagingAuthorizationService(messagingSecurityService.getMessagingAuthorizationService());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, methodName);
    }

    /**
     * Check if Messaging Security is enabled or not.
     * This is determined by the existence of the MessagingSecurityService.
     * If MessagingSecurity Service is not null, MessagingSecurity is enabled
     *
     * @return
     *         true : If MessasingSecurity is enabled
     *         false: If MessagingSecurity is disabled
     */
    @Override
    public boolean isMessagingSecure() {
        final String methodName = "isMessagingSecure";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, methodName, this);

        boolean result = false;
        if (messagingSecurityService != null)
            result = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, methodName, result);
        return result;
    }

    /**
     * Create a Unauthenticated Subject
     * When Messaging Security is disabled, we will create a blank subject
     * This should be called only when Messaging Security is disabled
     *
     * @return
     *         Subject: A Unauthenticated Subject
     */
    @Override
    public Subject createUnauthenticatedSubject() {

        SibTr.entry(tc, CLASS_NAME + "createUnauthenticatedSubject");
        if (unauthenticatedSubject == null) {
            unauthenticatedSubject = new Subject();
        }

        SibTr.exit(tc, CLASS_NAME + "createUnauthenticatedSubject", unauthenticatedSubject);
        return unauthenticatedSubject;
    }

    /**
     * Get Proxy Authentication instance
     *
     * @return
     *         Authentication
     */
    @Override
    public Authentication getAuthenticationInstance() {
        return authentication;
    }

    /**
     * Get Proxy Authorization instance
     *
     * @return
     */
    @Override
    public Authorization getAuthorizationInstance() {
       return authorization;
    }

    /**
     * Get Unique User name for the Subject
     *
     * @param subject
     * @return
     * @throws MessagingSecurityException
     */
    @Override
    public String getUniqueUserName(Subject subject) throws MessagingSecurityException {
        if (!isMessagingSecure()) {
            return "";
        } else {
            return messagingSecurityService.getUniqueUserName(subject);
        }
    }

    /**
     * Check if the subject passed is Authenticated
     * <ul>
     * <li> If Messaging Security is disabled, it returns false
     * <li> If Messaging Security is enabled, it calls MessagingAuthenticationService
     * from Messaging Security component </li>
     *
     * @param subject
     *            Subject which need to be checked
     * @return
     *         true : If the Subject is unauthenticated
     *         false: If the Subject is authenticated
     */
    @Override
    public boolean isUnauthenticated(Subject subject) throws MessagingAuthenticationException {

        SibTr.entry(tc, CLASS_NAME + "isUnauthenticated", subject);
        boolean result = false;
        if (!isMessagingSecure()) {
            result = false;
        } else {
            try {
                result = messagingSecurityService.isUnauthenticated(subject);
            } catch (Exception e) {
                throw new MessagingAuthenticationException(e.getMessage());
            }
        }

        SibTr.exit(tc, CLASS_NAME + "isUnauthenticated", subject);
        return result;
    }

    @Override
    public String toString() {
        return (messagingSecurityService == null?"-":"+") + super.toString();
    }
}
