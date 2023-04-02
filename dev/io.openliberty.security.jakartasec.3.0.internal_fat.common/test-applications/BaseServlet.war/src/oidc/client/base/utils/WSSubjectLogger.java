/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package oidc.client.base.utils;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class WSSubjectLogger {

    protected String caller = null;
    HttpServletRequest req;

    public WSSubjectLogger(HttpServletRequest request, String callingClass) {

        req = request;
        caller = callingClass;

    }

    public void printProgrammaticApiValues(ServletOutputStream ps) throws IOException {

        ServletLogger.printSeparator(ps);
        printBasicInfo(ps);
        printUserRoleInfo(ps);
        printCookies(ps);
        try {
            Subject callerSubject = WSSubject.getCallerSubject();
            printCallerSubjectInfo(callerSubject, ps);
            printCustomCacheKey(callerSubject, ps);
        } catch (NoClassDefFoundError ne) {
            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
            ServletLogger.printLine(ps, caller, "NoClassDefFoundError for SubjectManager: " + ne);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        ServletLogger.printSeparator(ps);
    }

    protected void printBasicInfo(ServletOutputStream ps) throws IOException {
        ServletLogger.printLine(ps, caller, ServletMessageConstants.GET_REQUEST_URL + req.getRequestURL().toString());
        ServletLogger.printLine(ps, caller, ServletMessageConstants.GET_AUTH_TYPE + req.getAuthType());
        ServletLogger.printLine(ps, caller, ServletMessageConstants.GET_REMOTE_USER + req.getRemoteUser());
        ServletLogger.printLine(ps, caller, ServletMessageConstants.GET_USER_PRINCIPAL + req.getUserPrincipal());
        if (req.getUserPrincipal() != null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.GET_USER_PRINCIPAL_GET_NAME + req.getUserPrincipal().getName());
        }
    }

    protected void printUserRoleInfo(ServletOutputStream ps) throws IOException {
        ServletLogger.printLine(ps, caller, ServletMessageConstants.IS_USER_IN_EMPLOYEE_ROLE + req.isUserInRole("Employee"));
        ServletLogger.printLine(ps, caller, ServletMessageConstants.IS_USER_IN_MANAGER_ROLE + req.isUserInRole("Manager"));
    }

    protected void printCookies(ServletOutputStream ps) throws IOException {
        Cookie[] cookies = req.getCookies();
        ServletLogger.printLine(ps, caller, ServletMessageConstants.GETTING_COOKIES);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                ServletLogger.printLine(ps, caller, ServletMessageConstants.COOKIE + cookie.getName() + " " + ServletMessageConstants.VALUE + cookie.getValue());
            }
        }
    }

    protected void printCallerSubjectInfo(Subject callerSubject, ServletOutputStream ps) throws WSSecurityException, IOException {
        ServletLogger.printLine(ps, caller, "callerSubject: " + callerSubject);
        // Get the public credential from the CallerSubject
        if (callerSubject != null) {
            printSubjectCredentials(callerSubject, ps);
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.CALLER_CREDENTIAL + " " + ServletMessageConstants.NULL);
        }

        // getInvocationSubject for RunAs tests
        Subject runAsSubject = WSSubject.getRunAsSubject();
        ServletLogger.printLine(ps, caller, ServletMessageConstants.RUNAS_SUBJECT + runAsSubject);
    }

    protected void printSubjectCredentials(Subject callerSubject, ServletOutputStream ps) throws IOException {
        printPublicCredentials(callerSubject, ps);
        printPrivateCredentials(callerSubject, ps);
    }

    protected void printPublicCredentials(Subject callerSubject, ServletOutputStream ps) throws IOException {
        WSCredential callerCredential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
        if (callerCredential != null) {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.CALLER_CREDENTIAL + callerCredential);
        } else {
            ServletLogger.printLine(ps, caller, ServletMessageConstants.CALLER_CREDENTIAL + " " + ServletMessageConstants.NULL);
        }
    }

    /**
     * Prints the various private credentials of the subject. Can be overridden by extending classes to print specific types of
     * private credentials (JWTs, UserProfile, etc.).
     */
    protected void printPrivateCredentials(Subject callerSubject, ServletOutputStream ps) throws IOException {
        // To be overridden, if desired
    }

    protected void printCustomCacheKey(Subject callerSubject, ServletOutputStream ps) throws IOException {
        // Check for cache key for hashtable login test. Will return null otherwise
        String customCacheKey = null;
        if (callerSubject != null) {
            customCacheKey = getCustomCacheKeyFromSubjectCustomProps(callerSubject);
            if (customCacheKey == null) {
                customCacheKey = getCustomCacheKeyFromSsoToken(callerSubject, ps);
            }
        }
        ServletLogger.printLine(ps, caller, ServletMessageConstants.CUSTOM_CACHE_KEY + customCacheKey);
    }

    protected String getCustomCacheKeyFromSubjectCustomProps(Subject callerSubject) throws IOException {
        String[] properties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(callerSubject, properties);
        if (customProperties != null) {
            return (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
        }
        return null;
    }

    protected String getCustomCacheKeyFromSsoToken(Subject callerSubject, ServletOutputStream ps) throws IOException {
        SingleSignonToken ssoToken = getSsoToken(callerSubject, ps);
        if (ssoToken != null) {
            String[] attrs = ssoToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
            if (attrs != null && attrs.length > 0) {
                return attrs[0];
            }
        }
        return null;
    }

    /**
     * Gets the SSO token from the subject.
     *
     * @param subject
     *            {@code null} is not supported.
     */
    protected SingleSignonToken getSsoToken(Subject subject, ServletOutputStream ps) throws IOException {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        ServletLogger.printLine(ps, caller, "Number of SSO token: " + ssoTokens.size());
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

}