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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import multiple.ham.qualifiers.Admin;
import multiple.ham.qualifiers.Operator;
import multiple.ham.qualifiers.Tester;
import multiple.ham.qualifiers.User;

/**
 * Implementation of the HttpAuthenticationMechanismHandler interface.
 * This class selects the highest priority HttpAuthenticationMechanism
 * and delegates authentication operations to it.
 */

@Default
@Priority(100000)
@ApplicationScoped
public class CustomHAMHandler implements HttpAuthenticationMechanismHandler {

    private String cachedSimpleName = "";

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
//	@Inject @Default // do not enable
    private HttpAuthenticationMechanism defaultHAM;
//	@Inject // do not enable
    private HttpAuthenticationMechanism justHAM;

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
        } else if (defaultHAM != null) {
            ham = defaultHAM;
        } else if (justHAM != null) {
            ham = justHAM;
        }

        if (ham != null) {
            // this has never worked as intended
//    		inspectHAM("HAM", ham);
        }
        return ham;
    }

    private Class<?> getActualClass(Object instance) {
        Class<?> clazz = instance.getClass();

        // If it's a CDI proxy, get the superclass (the actual implementation)
        if (clazz.getName().contains("$Proxy$") || clazz.getName().contains("$$")) {
            return clazz.getSuperclass();
        }

        return clazz;
    }

    private void inspectHAM(String name, HttpAuthenticationMechanism ham) {
        try {
            // Get the actual implementation class, not the CDI proxy
            Class<?> actualClass = getActualClass(ham);
            System.out.println(name + " (" + actualClass.toString() + "):");

            Field propsField = null;
            try {
                propsField = actualClass.getDeclaredField("qualifiedProperties");
            } catch (Exception e) {
                System.out.println(name + ": Unable to inspect - " + e.getMessage());
            }

            Field realmField = null;
            try {
                realmField = actualClass.getDeclaredField("realmName");
            } catch (Exception e) {
                System.out.println(name + ": Unable to inspect - " + e.getMessage());
            }

            if (realmField != null) {
                realmField.setAccessible(true);
                String realmName = (String) realmField.get(ham);
                System.out.println("====> and realmName is [" + realmName + "].");
            }

            if (propsField != null) {
                propsField.setAccessible(true);
                Properties props = (Properties) propsField.get(actualClass);
                if (props != null) {
                    props.forEach((key, value) -> System.out.println("  " + key + " = " + value));
                } else {
                    System.out.println("  No qualified properties");
                }
            }
        } catch (Exception e) {
            System.out.println(name + ": Unable to inspect - " + e.getMessage());
        }
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {

        System.out.println("######## found adminHAM of [" + ((adminHAM == null) ? "null" : adminHAM.toString()) + "].");
        System.out.println("######## found userHAM of [" + ((userHAM == null) ? "null" : userHAM.toString()) + "].");
        System.out.println("######## found operatorHAM of [" + ((operatorHAM == null) ? "null" : operatorHAM.toString()) + "].");
        System.out.println("######## found testerHAM of [" + ((testerHAM == null) ? "null" : testerHAM.toString()) + "].");
        System.out.println("######## found defaultHAM of [" + ((defaultHAM == null) ? "null" : defaultHAM.toString()) + "].");
        System.out.println("######## found justHAM of [" + ((justHAM == null) ? "null" : justHAM.toString()) + "].");

        HttpAuthenticationMechanism authMech = getHighestPriorityAuthMechanism();
        if (authMech == null) {
            System.err.println("No HttpAuthenticationMechanism available");
            return AuthenticationStatus.SEND_FAILURE;
        }

        String simpleName = getSimpleName(authMech);
        if (simpleName.equals(cachedSimpleName) == false) {
            System.out.println("The fixed HttpAuthenticationMechanism being used is: " + getSimpleName(authMech));
            cachedSimpleName = new String(simpleName);
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

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {

        HttpAuthenticationMechanism authMech = getHighestPriorityAuthMechanism();
        if (authMech == null) {
            System.err.println("No HttpAuthenticationMechanism available");
            return AuthenticationStatus.SEND_FAILURE;
        }

        System.out.println("Found the highest priority HttpAuthenticationMechanism: " + getSimpleName(authMech));

        // Use privileged action for security sensitive operations
        PrivilegedAction<AuthenticationStatus> action = new PrivilegedAction<AuthenticationStatus>() {
            @Override
            public AuthenticationStatus run() {
                try {
                    System.out.println("Delegating secureResponse to: " + authMech.getClass().getName());
                    return authMech.secureResponse(request, response, httpMessageContext);
                } catch (Exception e) {
                    System.err.println("Exception during secureResponse: " + e.getMessage());
                    return AuthenticationStatus.SEND_FAILURE;
                }
            }
        };

        return AccessController.doPrivileged(action);
    }

    @Override
    public void cleanSubject(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {
        HttpAuthenticationMechanism authMech = getHighestPriorityAuthMechanism();

        if (authMech == null) {
            System.err.println("No HttpAuthenticationMechanism available");
            return;
        }

        System.out.println("Found the highest priority HttpAuthenticationMechanism: " + getSimpleName(authMech));

        // Use privileged action for security sensitive operations
        PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    System.out.println("Delegating cleanSubject to: " + authMech.getClass().getName());
                    authMech.cleanSubject(request, response, httpMessageContext);
                } catch (Exception e) {
                    System.err.println("Exception during cleanSubject: " + e.getMessage());
                }
                return null;
            }
        };

        AccessController.doPrivileged(action);
    }

    private String getSimpleName(Object anyObject) {
        if (anyObject == null) {
            return "null";
        }
        return anyObject.getClass().getSimpleName().split("\\$")[0];
    }
}