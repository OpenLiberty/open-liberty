/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.test.servlets;

import java.util.Map;

/**
 *
 */
public interface ServletClient {

    // Define constants for the urlPatterns defined in web.xml
    public static final String UNPROTECTED_NO_SECURITY_CONSTRAINT = "/UnprotectedSimpleServlet";
    public static final String UNPROTECTED_NO_AUTH_CONSTRAINT = "/UnprotectedNoAuthConstraintServlet";
    public static final String PROTECTED_SIMPLE = "/SimpleServlet";
    public static final String PROTECTED_ALL_ROLE = "/AllRoleServlet";
    public static final String PROTECTED_EVERYONE_ROLE = "/EveryoneRoleServlet";
    public static final String PROTECTED_EMPLOYEE_ROLE = "/EmployeeRoleServlet";
    public static final String PROTECTED_MANAGER_ROLE = "/ManagerRoleServlet";
    public static final String PROTECTED_ACCESS_PRECLUDED = "/EmptyConstraintServlet";
    public static final String PROTECTED_OVERLAP_ALL_ACCESS = "/OverlapNoConstraintServlet";
    public static final String PROTECTED_OVERLAP_ACCESS_PRECLUDED = "/OverlapNoRoleServlet";
    public static final String PROTECTED_MATCH_ANY_PATTERN = "/MatchAny";
    public static final String PROTECTED_SPECIAL_ANY_ROLE_AUTH = "/StarConstraintServlet";
    public static final String PROTECTED_SPECIAL_ALL_AUTH = "/StarStarConstraintServlet";
    public static final String SSL_SECURED_SIMPLE = "/SecureSimpleServlet";

    public static final String UNPROTECTED_PROGRAMMATIC_API_SERVLET = "/UnprotectedProgrammaticAPIServlet";
    public static final String PROTECTED_PROGRAMMATIC_API_SERVLET = "/ProgrammaticAPIServlet";
    public static final String PROTECTED_JAAS_SERVLET = "/JAASServlet";
    public static final String PROTECTED_SPNEGO_SERVLET = "/SpnegoServlet";

    public static final String PROTECTED_AUTHENTICATION_REDIRECT_SERVLET = "/AuthenticateRedirectServlet";
    public static final String UNPROTECTED_AUTHENTICATION_REDIRECT_SERVLET = "/UnprotectedAuthenticateRedirectServlet";

    public static final String OMISSION_BASIC = "/OmissionBasic";
    public static final String OVERLAP_CUSTOM_METHOD_SERVLET = "/OverlapCustomMethodServlet";
    public static final String CUSTOM_METHOD_SERVLET = "/CustomMethodServlet";

    // Keys to help readability of the test
    public static final boolean IS_MANAGER_ROLE = true;
    public static final boolean NOT_MANAGER_ROLE = false;
    public static final boolean IS_EMPLOYEE_ROLE = true;
    public static final boolean NOT_EMPLOYEE_ROLE = false;

    /**
     * @return
     */
    public abstract String getContextRoot();

    /**
     * Resets the client state to ensure the next connection
     * behaves as if there were no previous connections.
     */
    public abstract void resetClientState();

    /**
     * Releases the client. Calling this method is optional
     * but it will release all resources associated with the client.
     */
    public abstract void releaseClient();

    /**
     * Access an unavailable URL pattern that is under of the context root.
     *
     * @param urlPattern
     *                       URL pattern that is under the context root.
     * @return servlet response text
     */
    public abstract String accessUnavailableServlet(String urlPattern);

    /**
     * Access an unprotected URL pattern that is under of the context root.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @return servlet response text
     */
    public abstract String accessUnprotectedServlet(String urlPattern);

    /**
     * Access a protected URL pattern that is under the context root.
     * The HTTP get method is uncovered and <deny-uncovered-http-methods/>
     * is specified in the web.xml.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param user
     *                       user to authenticate as
     * @param password
     *                       password to authenticate with
     * @return true if access was denied with 403
     */
    public abstract boolean accessDeniedHttpMethodServlet(String urlPattern, String user, String password);

    /**
     * Access a protected (and access precluded) URL pattern that is part of the
     * context root. No authentication challenge should be presented since
     * access will never be permitted.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param user
     *                       user to authenticate as
     * @param password
     *                       password to authenticate with
     * @return servlet response text, null if access not granted
     */
    public abstract boolean accessPrecludedServlet(String urlPattern);

    /**
     * Access a protected URL pattern that is part of the context root.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param user
     *                       user to authenticate as
     * @param password
     *                       password to authenticate with
     * @return servlet response text, null if access not granted
     */
    public abstract String accessProtectedServletWithAuthorizedCredentials(String urlPattern, String user, String password);

    /**
     * Access a protected URL pattern that is part of the context root. Access
     * is expected to be rejected as the user is not authorized.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param user
     *                       user to authenticate as
     * @param password
     *                       password to authenticate with
     * @return true if access was denied with 403
     */
    public abstract boolean accessProtectedServletWithUnauthorizedCredentials(String urlPattern, String user, String password);

    /**
     * Access a protected URL pattern that is part of the context root. Access
     * is expected to be rejected as the credentials are not valid.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param user
     *                       user to authenticate as
     * @param password
     *                       password to authenticate with
     * @return true if access was denied with 401
     */
    public abstract boolean accessProtectedServletWithInvalidCredentials(String urlPattern, String user, String password);

    /**
     * Access a protected URL pattern that is part of the context root. Access
     * is expected to be rejected as the registry is not valid.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param user
     *                       user to authenticate as
     * @param password
     *                       password to authenticate with
     * @return true if access was denied with 401
     */
    public abstract boolean accessProtectedServletWithInvalidRegistry(String urlPattern, String user, String password);

    /**
     * Access a protected URL pattern that is part of the context root using
     * the passed valid headers.
     *
     * @param urlPattern
     *                       URL pattern that is within the context root.
     * @param headers
     *                       Map of header names and values to be included in the request
     * @return servlet response text, null if access not granted
     */
    public abstract String accessProtectedServletWithValidHeaders(String urlPattern, Map<String, String> headers);

    /**
     * Access a protected URL pattern that is part of the context root using
     * the passed valid headers.
     *
     * @param urlPattern
     *                               URL pattern that is within the context root.
     * @param headers
     *                               Map of header names and values to be included in the request
     * @param ignoreErrorContent
     *                               If true, any HTTP response content received with a non-200 status code will be returned as null
     * @return servlet response text. If ignoreErrorContent is true, null is returned if access not granted
     */
    public abstract String accessProtectedServletWithValidHeaders(String urlPattern, Map<String, String> headers, Boolean ignoreErrorContent);

    /**
     * Access a protected URL pattern that is part of the context root using
     * the passed headers that are not valid.
     *
     * @param urlPattern
     *                       URL pattern that is within the context root.
     * @param headers
     *                       Map of header names and values to be included in the request
     * @return servlet response text, null if access not granted
     */
    public abstract String accessProtectedServletWithInvalidHeaders(String urlPattern, Map<String, String> headers);

    /**
     * Access a protected URL pattern that is part of the context root using
     * the passed headers that are not valid.
     *
     * @param urlPattern
     *                               URL pattern that is within the context root.
     * @param headers
     *                               Map of header names and values to be included in the request
     * @param ignoreErrorContent
     *                               If true, any HTTP response content received with a non-200 status code will be returned as null
     * @return
     */
    public abstract String accessProtectedServletWithInvalidHeaders(String urlPattern, Map<String, String> headers, boolean ignoreErrorContent);

    /**
     * Access a protected URL pattern that is part of the context root using
     * the passed headers that are not valid.
     *
     * @param urlPattern
     *                               URL pattern that is within the context root.
     * @param headers
     *                               Map of header names and values to be included in the request
     * @param ignoreErrorContent
     *                               If true, any HTTP response content received with a non-200 status code will be returned as null
     * @param expectedStatusCode
     *                               Expected status code of the response
     * @return servlet response text. If ignoreErrorContent is true, null is returned if access not granted
     */
    public abstract String accessProtectedServletWithInvalidHeaders(String urlPattern, Map<String, String> headers, boolean ignoreErrorContent, int expectedStatusCode);

    /**
     * Access a URL pattern that requires SSL.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @return true if access was denied with 403
     */
    public abstract boolean accessSSLRequiredSevlet(String urlPattern);

    /**
     * Override the default SSO cookie name
     */
    public abstract void setSSOCookieName(String ssoCookieName);

    public abstract String getCookieFromLastLogin();

    /**
     * Access a protected URL pattern that is part of the context root.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param cookie
     *                       cookie to authenticate with
     * @return servlet response text, null if access not granted
     */
    public abstract String accessProtectedServletWithAuthorizedCookie(String urlPattern,
                                                                      String cookie);

    /**
     * Access a protected URL pattern that is part of the context root. Access
     * is expected to be rejected as the user is not authorized.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param cookie
     *                       cookie to authenticate with
     * @return true if access was denied with 403
     */
    public abstract boolean accessProtectedServletWithUnauthorizedCookie(
                                                                         String urlPattern, String cookie);

    /**
     * Access a protected URL pattern that is part of the context root. Access
     * is expected to be rejected as the cookie is not valid.
     *
     * @param urlPattern
     *                       URL pattern that is under of the context root.
     * @param cookie
     *                       cookie to authenticate with
     * @return true if access was denied due to invalid cookie
     */
    public abstract boolean accessProtectedServletWithInvalidCookie(String urlPattern,
                                                                    String cookie);

    /**
     * Check for the specified password in logs and trace, IF we are running in
     * the FAt environment (we have a handle to a LibertyServer object). This
     * method does not return and will trigger a JUnit failre if the password is
     * found.
     *
     * This is called explicitly, and not implicitly, because implicit calls are
     * repeated and really expensive.
     *
     * @param password
     *                     password string to search for
     * @throws Exception
     */
    public abstract void checkForPasswordsInLogsAndTrace(String password) throws Exception;

    /**
     * Verify the default values for an unauthenticated response.
     *
     * @param response
     *                     Servlet response text
     * @return true is things verified properly, false otherwise.
     */
    public abstract boolean verifyUnauthenticatedResponse(String response);

    /**
     * Verify the expected values for an authenticated response.
     *
     * @param response
     *                                 Servlet response text
     * @param userName
     *                                 Expected user name
     * @param isUserInEmployeeRole
     *                                 If the user should be in the employee role
     * @param isUserInManagerRole
     *                                 If the user should be in the manager role
     * @return true is things verified properly, false otherwise.
     */
    public abstract boolean verifyResponse(String response, String userName,
                                           boolean isUserInEmployeeRole, boolean isUserInManagerRole);

    /**
     * Verify the expected values for an authenticated response.
     * Check for a specified isUserInRole role.
     *
     * @param response
     *                                  Servlet response text
     * @param userName
     *                                  Expected user name
     * @param isUserInEmployeeRole
     *                                  If the user should be in the employee role
     * @param isUserInManagerRole
     *                                  If the user should be in the manager role
     * @param specifiedRole
     *                                  Specified role to servlet for isUserInRole check
     * @param isUserInSpecifiedRole
     *                                  If the user should be in the specified role
     * @return true is things verified properly, false otherwise.
     */
    public abstract boolean verifyResponse(String response, String userName,
                                           boolean isUserInEmployeeRole, boolean isUserInManagerRole,
                                           String specifiedRole, boolean isUserInSpecifiedRole);

    /**
     * @param urlPattern
     * @param expectedException
     * @return
     */
    String accessUnavailableServlet(String urlPattern, Class<?> expectedException);

    /**
     * @param urlPattern
     * @param expectedException
     * @return
     */
    String accessUnavailableServlet(String urlPattern, Class<?>[] expectedException);

    /**
     * @param isJaccScenario
     */
    void setJaccValidation(boolean isJaccScenario);
}