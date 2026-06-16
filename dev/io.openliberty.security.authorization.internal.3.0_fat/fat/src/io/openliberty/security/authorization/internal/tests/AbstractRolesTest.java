/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.internal.tests;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import com.ibm.websphere.simplicity.PortType;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.authorization.fat.TestUtil;
import junit.framework.Assert;

public class AbstractRolesTest {

    enum InRoleOutput {
        DECLARED_IN_COMPONENT, ALL
    }

    public static final String WITH_AUTHZ_ID = "withAuthz";
    public static final String WITH_POLICY_ID = "withPolicy";

    public static HttpResponse<String> sendGetRequest(LibertyServer server, HttpClient client, String uri) throws Exception {
        URI requestURI = URI.create("http://" + server.getHostname() + ":" + server.getPort(PortType.WC_defaulthost) + uri);
        HttpRequest request = HttpRequest.newBuilder().uri(requestURI).GET().build();
        HttpResponse<String> response = client.send(request,
                                                    HttpResponse.BodyHandlers.ofString());

        return response;
    }

    public static HttpClient createHttpClient() {
        HttpClient client = HttpClient.newBuilder().build();
        return client;
    }

    public static HttpClient createHttpClient(String user, String password) {
        HttpClient client = HttpClient.newBuilder().authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        }).build();
        return client;
    }

    public static void validateResponse(String response, String user, Set<String> declaredRolesExpected, Set<String> allRolesExpected, boolean isStarStarMapped,
                                        InRoleOutput componentOutput) {
        StringBuilder errorMessage = new StringBuilder();
        String userInResponse = getValue(response, "User Principal:", errorMessage, true);
        if (userInResponse != null && !userInResponse.equals(user)) {
            errorMessage.append("User Principal in response was ").append(userInResponse).append(", but expected ").append(user).append('\n');
        }
        userInResponse = getValue(response, "Caller Principal:", errorMessage, true);
        if (userInResponse != null && !userInResponse.equals(user)) {
            errorMessage.append("Caller Principal in response was ").append(userInResponse).append(", but expected ").append(user).append('\n');
        }

        /**
         * Presently Component context isUserInRole() returns true for any declared roles, whereas
         * SecurityContext.isCallerInRole is returning true for all roles that are mapped even if they are not
         * declared roles in the servlet.
         */

        Set<String> componentRolesExpected = declaredRolesExpected;
        if (componentOutput == InRoleOutput.ALL) {
            componentRolesExpected = allRolesExpected;
        }
        for (String role : TestUtil.allRoles) {
            String inRoleString = getValue(response, "User in role " + role + ":", errorMessage, true);
            if (inRoleString != null) {
                Boolean inRoleBoolean = "true".equals(inRoleString) ? Boolean.TRUE : "false".equals(inRoleString) ? Boolean.FALSE : null;
                boolean expectedUserInRole = componentRolesExpected.contains(role);
                if (inRoleBoolean == null || expectedUserInRole != inRoleBoolean.booleanValue()) {
                    errorMessage.append("User in role in response for role ").append(role).append(" was ").append(inRoleString).append(", but expected ").append(expectedUserInRole).append('\n');
                }
            }
            inRoleString = getValue(response, "Caller in role " + role + ":", errorMessage, true);
            if (inRoleString != null) {
                Boolean inRoleBoolean = "true".equals(inRoleString) ? Boolean.TRUE : "false".equals(inRoleString) ? Boolean.FALSE : null;
                boolean expectedCallerInRole = allRolesExpected.contains(role);
                if (inRoleBoolean == null || expectedCallerInRole != inRoleBoolean.booleanValue()) {
                    errorMessage.append("Caller in role in response for role ").append(role).append(" was ").append(inRoleString).append(", but expected ").append(expectedCallerInRole).append('\n');
                }
            }
        }
        String inRoleString = getValue(response, "User in role **:", errorMessage, true);
        if (inRoleString != null) {
            Boolean inRoleBoolean = "true".equals(inRoleString) ? Boolean.TRUE : "false".equals(inRoleString) ? Boolean.FALSE : null;
            boolean expectedUserInRole = !user.equals("Not authenticated");
            if (inRoleBoolean == null || expectedUserInRole != inRoleBoolean.booleanValue()) {
                errorMessage.append("User in role in response for role ** was ").append(inRoleString).append(", but expected ").append(expectedUserInRole).append('\n');
            }
        }
        inRoleString = getValue(response, "Caller in role **:", errorMessage, true);
        if (inRoleString != null) {
            Boolean inRoleBoolean = "true".equals(inRoleString) ? Boolean.TRUE : "false".equals(inRoleString) ? Boolean.FALSE : null;
            boolean expectedCallerInRole = !user.equals("Not authenticated");
            if (inRoleBoolean == null || expectedCallerInRole != inRoleBoolean.booleanValue()) {
                errorMessage.append("Caller in role in response for role ** was ").append(inRoleString).append(", but expected ").append(expectedCallerInRole).append('\n');
            }
        }

        String declaredRolesString = getValue(response, "Declared Roles:", errorMessage, true);
        if (declaredRolesString != null) {
            setComparison(declaredRolesString, "Declared roles", declaredRolesExpected, errorMessage);
        }

        boolean policyEnabled = RepeatTestFilter.isRepeatActionActive(WITH_POLICY_ID);

        String isStarStarMappedString = getValue(response, "Is ** mapped:", errorMessage, policyEnabled);
        if (isStarStarMappedString != null) {
            Boolean inMappedBoolean = "true".equals(isStarStarMappedString) ? Boolean.TRUE : "false".equals(isStarStarMappedString) ? Boolean.FALSE : null;
            if (inMappedBoolean == null || isStarStarMapped != inMappedBoolean.booleanValue()) {
                errorMessage.append("Is ** mapped in response was ").append(isStarStarMappedString).append(", but expected ").append(isStarStarMapped).append('\n');
            }
        }

        String allRolesString = getValue(response, "PrincipalMapper Roles for subject:", errorMessage, policyEnabled);
        if (allRolesString != null) {
            setComparison(allRolesString, "PrincipalMapper roles for subject", allRolesExpected, errorMessage);
        }
        if (errorMessage.length() > 0) {
            System.out.println(response);
            Assert.fail("Response did not contain expected value \n" + errorMessage.toString());
        }
    }

    private static void setComparison(String valueFromResponse, String messagePrefix, Set<String> expectedValue, StringBuilder errorMessage) {
        Set<String> responseSet = valueFromResponse.equals("") ? Set.of() : Set.of(valueFromResponse.split(", "));
        boolean mismatch = responseSet.size() != expectedValue.size();
        if (!mismatch) {
            for (String expectedRole : expectedValue) {
                if (!responseSet.contains(expectedRole)) {
                    mismatch = true;
                    break;
                }
            }
        }
        if (mismatch) {
            errorMessage.append(messagePrefix).append(" in response was ").append(valueFromResponse).append(", but expected ").append(TestUtil.setToString(expectedValue)).append('\n');
        }

    }

    private static String getValue(String response, String prefix, StringBuilder errorMessage, boolean expectedValue) {
        int index = response.indexOf(prefix);
        if (index >= 0) {
            if (!expectedValue) {
                errorMessage.append(prefix).append(" found in response when it wasn't expected\n");
            }
            int startIndex = response.indexOf(':', index);
            int endIndex = response.indexOf('\n', index);
            if (startIndex >= 0 && endIndex >= 0) {
                return response.substring(startIndex + 1, endIndex).trim();
            }
        }
        if (expectedValue) {
            errorMessage.append(prefix).append(" not found in response\n");
        }
        return null;
    }

    public static void installJaccUserFeature(LibertyServer myServer) throws Exception {
        myServer.installUserBundle("io.openliberty.security.authorization.jacc.testprovider_3.0");
        myServer.installUserFeature("jaccTestProvider-3.0");
        myServer.installUserBundle("io.openliberty.security.authorization.jacc.testprovider.spec_3.0");
        myServer.installUserFeature("authzTestProvider-3.0");
    }

    public static void uninstallJaccUserFeature(LibertyServer myServer) throws Exception {
        myServer.uninstallUserBundle("io.openliberty.security.authorization.jacc.testprovider_3.0");
        myServer.uninstallUserFeature("jaccTestProvider-3.0");
        myServer.uninstallUserBundle("io.openliberty.security.authorization.jacc.testprovider.spec_3.0");
        myServer.uninstallUserFeature("authzTestProvider-3.0");
    }
}
