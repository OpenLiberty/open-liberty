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
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import multiple.ham.common.qualifiers.Admin;
import multiple.ham.common.qualifiers.Operator;
import multiple.ham.common.qualifiers.Tester;
import multiple.ham.common.qualifiers.User;

/**
 * Implementation of the HttpAuthenticationMechanismHandler interface.
 * This class defines a custom priority among the injected qualifiers
 * and prints out their injections.
 */

@Default
@ApplicationScoped
public class CustomHAMHandler implements HttpAuthenticationMechanismHandler {

    @Inject
    @Admin
    private HttpAuthenticationMechanism adminHAM;
    @Inject
    @User
    private HttpAuthenticationMechanism userHAM;
    @Inject
    @Operator
    private HttpAuthenticationMechanism operatorHAM;
    @Inject
    @Tester
    private HttpAuthenticationMechanism testerHAM;

    public CustomHAMHandler() {
    }

    @PostConstruct
    public void init() {
    }

    protected HttpAuthenticationMechanism getHighestPriorityAuthMechanism() {

        HttpAuthenticationMechanism ham = null;
        if (adminHAM != null) {
            ham = adminHAM;
        } else if (userHAM != null) {
            ham = userHAM;
        } else if (operatorHAM != null) {
            ham = operatorHAM;
        } else if (testerHAM != null) {
            ham = testerHAM;
        }

        return ham;
    }

    @SuppressWarnings("removal")
    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {

        System.out.println("######## found adminHAM of [" + ((adminHAM == null) ? "null" : adminHAM.toString()) + "].");
        System.out.println("######## found userHAM of [" + ((userHAM == null) ? "null" : userHAM.toString()) + "].");
        System.out.println("######## found operatorHAM of [" + ((operatorHAM == null) ? "null" : operatorHAM.toString()) + "].");
        System.out.println("######## found testerHAM of [" + ((testerHAM == null) ? "null" : testerHAM.toString()) + "].");

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

    private String getSimpleName(Object anyObject) {
        if (anyObject == null) {
            return "null";
        }
        return anyObject.getClass().getSimpleName().split("\\$")[0];
    }

}