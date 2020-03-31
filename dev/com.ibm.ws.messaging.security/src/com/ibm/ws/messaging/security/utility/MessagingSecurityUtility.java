/*******************************************************************************
 * Copyright (c) 2012, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.security.utility;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.messaging.security.MSTraceConstants;
import com.ibm.ws.messaging.security.MessagingSecurityConstants;
import com.ibm.ws.messaging.security.MessagingSecurityException;
import com.ibm.ws.messaging.security.authorization.internal.MessagingAuthorizationServiceImpl;
import com.ibm.ws.messaging.security.internal.MessagingSecurityServiceImpl;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Utility Class for Liberty Messaging Security
 * 
 * @author Sharath Chandra
 * 
 */
public class MessagingSecurityUtility implements MessagingSecurityConstants {

    // Trace component for the MessagingSecurityUtility class
    private static TraceComponent tc = SibTr.register(MessagingAuthorizationServiceImpl.class,
                                                      MSTraceConstants.MESSAGING_SECURITY_TRACE_GROUP,
                                                      MSTraceConstants.MESSAGING_SECURITY_RESOURCE_BUNDLE);

    // Absolute class name along with the package name, used for tracing
    private static final String CLASS_NAME = "com.ibm.ws.messaging.security.utility.MessagingSecurityUtility.";

    private static SubjectHelper subjectHelper = new SubjectHelper();

    /**
     * Create the AuthenticationData from the UserName
     * 
     * @param userName
     * @return
     */
    public static AuthenticationData createAuthenticationData(String userName, UserRegistry userRegistry) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "createAuthenticationData", userName);
        }
        AuthenticationData authData = new WSAuthenticationData();
        if (userName == null) {
            userName = "";
        }
        String realm = getDefaultRealm(userRegistry);
        authData.set(AuthenticationData.USERNAME, userName);
        authData.set(AuthenticationData.REALM, realm);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "createAuthenticationData", authData);
        }
        return authData;
    }

    /**
     * Create AuthenticationData object from the Token passed
     * 
     * @param token
     * @return
     */
    public static AuthenticationData createAuthenticationData(byte[] token) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "createAuthenticationData", token);
        }
        AuthenticationData authData = new WSAuthenticationData();
        authData.set(AuthenticationData.TOKEN, token);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "createAuthenticationData", authData);
        }
        return authData;
    }

    /**
     * Create AuthenticationData Object from the UserName and Password passed
     * 
     * @param userName
     * @param password
     * @return
     */
    public static AuthenticationData createAuthenticationData(String userName, String password) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "createAuthenticationData", new Object[] { userName, "Password Not Traced" });
        }
        AuthenticationData authData = new WSAuthenticationData();
        if (userName == null)
            userName = "";
        if (password == null)
            password = "";
        authData.set(AuthenticationData.USERNAME, userName);
        authData.set(AuthenticationData.PASSWORD, new ProtectedString(password.toCharArray()));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "createAuthenticationData", authData);
        }
        return authData;
    }

    /**
     * Create AuthenticationData object from the Certificate
     * 
     * @param certs
     * @param userRegistry
     * @return
     */
    public static AuthenticationData createAuthenticationData(Certificate[] certs, UserRegistry userRegistry) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "createAuthenticationData", certs);
        }
        AuthenticationData authData = new WSAuthenticationData();
        X509Certificate[] _certs;
        _certs = new X509Certificate[certs.length];
        for (int i = 0; i < certs.length; i++) {
            if (certs[i] instanceof X509Certificate) {
                _certs[i] = (X509Certificate) certs[i];
            } else {
                _certs = null;
                break;
            }
        }
        authData.set(AuthenticationData.CERTCHAIN, _certs);
        authData.set(AuthenticationData.REALM, getDefaultRealm(userRegistry));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "createAuthenticationData", authData);
        }
        return authData;
    }

    /**
     * Get the Default Realm
     * 
     * @param _userRegistry
     * @return
     */
    private static String getDefaultRealm(UserRegistry _userRegistry) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "getDefaultRealm");
        }
        String realm = DEFAULT_REALM;
        if (_userRegistry != null) {
            realm = _userRegistry.getRealm();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "getDefaultRealm", realm);
        }
        return realm;
    }

    /**
     * This method returns the unique name of the user that was being
     * authenticated. This is a best can do process and a user name may not be
     * available, in which case null should be returned. This method should not
     * return an empty string.
     * 
     * @param subject
     *            the WAS authenticated subject
     * 
     * @return The name of the user being authenticated.
     * @throws MessagingSecurityException
     */
    public static String getUniqueUserName(Subject subject) throws MessagingSecurityException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "getUniqueUserName", subject);
        }
        if (subject == null) {
            return null;
        }

        WSCredential cred = subjectHelper.getWSCredential(subject);
        String userName = null;
        if (cred != null) {
            try {
                userName = cred.getSecurityName();
            } catch (CredentialException ce) {
                throw new MessagingSecurityException(ce);
            } catch (CredentialDestroyedException e) {
                throw new MessagingSecurityException(e);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "getUniqueUserName", userName);
        }
        return userName;
    }

    public static List<String> getGroupsAssociatedToUser(String userName, MessagingSecurityServiceImpl messagingSecurityService) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "constructor", messagingSecurityService);
        }
        List<String> groupsAssociatedWithUser = new ArrayList<String>();
        UserRegistryService userRegistryService = messagingSecurityService.getSecurityService().getUserRegistryService();
        UserRegistry userRegistry;
        try {
            userRegistry = userRegistryService.getUserRegistry();
            groupsAssociatedWithUser = userRegistry.getGroupsForUser(userName);
        } catch (RegistryException e) {
            // TODO Sharath
        } catch (EntryNotFoundException e) {
            // TODO Sharath
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "constructor", messagingSecurityService);
        }
        return groupsAssociatedWithUser;
    }

    /**
     * Check if the Subject is Authenticated
     * 
     * @param subject
     * @return
     *         true if Subject is not authenticated
     */
    public static boolean isUnauthenticated(Subject subject) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "isUnauthenticated", subject);
        }
        boolean result = subjectHelper.isUnauthenticated(subject);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "isUnauthenticated", result);
        }
        return result;
    }

}
