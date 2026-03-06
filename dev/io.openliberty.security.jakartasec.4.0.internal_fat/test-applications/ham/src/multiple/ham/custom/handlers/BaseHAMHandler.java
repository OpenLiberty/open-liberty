/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package multiple.ham.custom.handlers;

import java.security.AccessController;
import java.security.PrivilegedAction;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abstract base class for the implementation of the HttpAuthenticationMechanismHandler interface.
 * This class defines a custom priority among the several different qualifiers and prints their instances.
 */
@Default
@ApplicationScoped
public abstract class BaseHAMHandler implements HttpAuthenticationMechanismHandler {

    /**
     * Get the test class for logging purposes.
     * Subclasses should override this to return their own class.
     */
    protected abstract Class<?> getTestClass();

    /**
     * Get the qualifier HAMs instances.
     * Subclasses must implement them to provide their own instances.
     */
    protected abstract HttpAuthenticationMechanism getAdminHAM();

    protected abstract HttpAuthenticationMechanism getUserHAM();

    protected abstract HttpAuthenticationMechanism getOperatorHAM();

    protected abstract HttpAuthenticationMechanism getTesterHAM();

    protected abstract HttpAuthenticationMechanism getDefaultHAM();

    public BaseHAMHandler() {
    }

    @PostConstruct
    public void init() {
    }

    /**
     * A default implementation of the prioritization among different HAMs with qualifiers
     * Unless no qualifier is specified, the default is returned
     *
     * @return the prioritized HttpAuthenticationMechanism
     */
    protected HttpAuthenticationMechanism getHighestPriorityAuthMechanism() {

        HttpAuthenticationMechanism ham = getDefaultHAM();
        if (getAdminHAM() != null) {
            ham = getAdminHAM();
        } else if (getUserHAM() != null) {
            ham = getUserHAM();
        } else if (getOperatorHAM() != null) {
            ham = getOperatorHAM();
        } else if (getTesterHAM() != null) {
            ham = getTesterHAM();
        }

        return ham;
    }

    @SuppressWarnings("removal")
    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {

        System.out.println("######## found adminHAM of [" + ((getAdminHAM() == null) ? "null" : getAdminHAM().toString()) + "].");
        System.out.println("######## found userHAM of [" + ((getUserHAM() == null) ? "null" : getUserHAM().toString()) + "].");
        System.out.println("######## found operatorHAM of [" + ((getOperatorHAM() == null) ? "null" : getOperatorHAM().toString()) + "].");
        System.out.println("######## found testerHAM of [" + ((getTesterHAM() == null) ? "null" : getTesterHAM().toString()) + "].");
        System.out.println("######## found defaultHAM of [" + ((getDefaultHAM() == null) ? "null" : getDefaultHAM().toString()) + "].");

        HttpAuthenticationMechanism authMech = getHighestPriorityAuthMechanism();
        if (authMech == null) {
            System.err.println("No HttpAuthenticationMechanism available");
            return AuthenticationStatus.SEND_FAILURE;
        } else {
            System.out.println("######## found Highest Priority HttpAuthenticationMechanism: " + getSimpleName(authMech));
        }

        // Use privileged action for security sensitive operations
        PrivilegedAction<AuthenticationStatus> action = new PrivilegedAction<AuthenticationStatus>() {
            @Override
            public AuthenticationStatus run() {
                try {
                    System.out.println("Delegating validateRequest to: " + authMech.getClass().getName());
                    return authMech.validateRequest(request, response, httpMessageContext);
                } catch (Exception e) {
                    System.err.println("Exception during validateRequest: " + e.getMessage());
                    return AuthenticationStatus.SEND_FAILURE;
                }
            }
        };

        return AccessController.doPrivileged(action);
    }

    protected String getSimpleName(Object anyObject) {
        if (anyObject == null) {
            return "null";
        }
        return anyObject.getClass().getSimpleName().split("\\$")[0];
    }

}