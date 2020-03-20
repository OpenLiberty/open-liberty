/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationException;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * RuntimeSecurityService class acts as a call back service to MessagingSecurity
 * Component. When the MessagingSecurity component is activated, it initializes
 * this service. When there are any modifications to MessagingSecurity component,
 * the services which are referred in this class will be reset.
 * 
 * RuntimeSecurityService is declared as Enum (Java 5 & onwards) to make this
 * service a Singleton instance
 * 
 * @author Sharath Chandra B
 * 
 */
public enum RuntimeSecurityService {

    // A Singleton instance of this Service
    SINGLETON_INSTANCE;

    // Trace component for the Authentication class
    private static TraceComponent tc = SibTr.register(RuntimeSecurityService.class,
                                                      MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
                                                      MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

    // Absolute class name along with the package used for tracing
    private static final String CLASS_NAME = "com.ibm.ws.messaging.security.RuntimeSecurityService.";

    // MessagingSecurityService variable which is null when Messaging Security 
    // is disabled, it will be initialized only when Messaging Security is enabled
    private MessagingSecurityService messagingSecurityService = null;

    // Proxy Authentication class
    private Authentication authentication = null;

    // Proxy Authorization class
    private Authorization authorization = null;

    private Subject unauthenticatedSubject = null;

    /**
     * This method resets all the variables declared in this Service.
     * When there are changes to Messaging Security Component, this method is called.
     * <ul>
     * <li> A MessagingSecurityService instance is sent when Messaging Security Service is enabled </li>
     * <li> Null is sent to this method, when Messaging Security Service is disabled/removed </li>
     * <ul>
     * 
     * @param messagingSecurityService
     */
    public synchronized void modifyMessagingServices(MessagingSecurityService messagingSecurityService) {

        SibTr.entry(tc, CLASS_NAME + "modifyMessagingServices", messagingSecurityService);
        this.messagingSecurityService = messagingSecurityService;
        if (messagingSecurityService != null) {
            getAuthenticationInstance().setMessagingAuthenticationService(messagingSecurityService.getMessagingAuthenticationService());
            getAuthorizationInstance().setMessagingAuthorizationService(messagingSecurityService.getMessagingAuthorizationService());
        } else {
            getAuthenticationInstance().setMessagingAuthenticationService(null);
            getAuthorizationInstance().setMessagingAuthorizationService(null);
        }

        SibTr.exit(tc, CLASS_NAME + "modifyMessagingServices");
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
    public boolean isMessagingSecure() {

        SibTr.entry(tc, CLASS_NAME + "isMessagingSecure");
        boolean result = false;
        if (messagingSecurityService != null)
            result = true;

        SibTr.exit(tc, CLASS_NAME + "isMessagingSecure", result);
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
    public Authentication getAuthenticationInstance() {
        if (authentication == null) {
            authentication = new Authentication();
        }
        return authentication;
    }

    /**
     * Get Proxy Authorization instance
     * 
     * @return
     */
    public Authorization getAuthorizationInstance() {
        if (authorization == null) {
            authorization = new Authorization();
        }
        return authorization;
    }

    /**
     * Get Unique User name for the Subject
     * 
     * @param subject
     * @return
     * @throws MessagingSecurityException
     */
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

}
