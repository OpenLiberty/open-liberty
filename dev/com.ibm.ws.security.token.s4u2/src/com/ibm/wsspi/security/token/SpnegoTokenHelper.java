/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.token;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.kerberos.auth.Krb5LoginModuleWrapper;
import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.token.internal.TraceConstants;
import com.ibm.ws.security.token.krb5.Krb5Helper;

/**
 * SpnegoTokenHelper
 * - utilities to help create a SPNEGO Token as Authorization header for outbound authentication purposes
 *
 * @author IBM Corporation
 * @version 1.1
 * @since 1.0
 * @ibm-api
 *
 */
public class SpnegoTokenHelper {
    private static final TraceComponent tc = Tr.register(SpnegoTokenHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /**
     * Build a SPNEGO Authorization string using a Kerberos credential within the current caller Java Subject.
     * The method will use that credential to request a SPNEGO token for a ServicePrincipalName (SPN) for the target service system.
     *
     * @param spn      - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - Whether the token includes delegatable GSSCredentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws WSSecurityException       - thrown when no caller Subject exists.
     * @throws GSSException              - thrown when SPNEGO token generation fails, when Subject is null, when the Subject
     *                                       does not contain Kerberos credentials, or when SPN is invalid.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     */
    public static String buildSpnegoAuthorizationFromCallerSubject(final String spn, final int lifetime,
                                                                   final boolean delegate) throws WSSecurityException, GSSException, PrivilegedActionException {

        Subject subject = WSSubject.getCallerSubject();
        if (subject == null) {
            subject = WSSubject.getRunAsSubject();
        }

        return buildSpnegoAuthorizationFromSubject(spn, subject, lifetime, delegate);
    }

    /**
     * Build a SPNEGO Authorization string using a Kerberos credential within the supplied Java Subject.
     * The method will use that credential to request a SPNEGO token for a ServicePrincipalName (SPN) for the target service system.
     *
     * @param spn      - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param subject  - Subject containing Kerberos credentials
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - whether the token includes delegatable GSSCredentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException              - thrown when SPNEGO token generation fails, when Subject is null, when the Subject
     *                                       does not contain Kerberos credentials, or when SPN is invalid.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     * @throws LoginException            - thrown when the Login fails with the supplied SPN.
     */
    public static String buildSpnegoAuthorizationFromSubject(final String spn, final Subject subject, final int lifetime,
                                                             final boolean delegate) throws GSSException, PrivilegedActionException {
        Krb5Helper.checkSpn(spn);
        return Krb5Helper.buildSpnegoAuthorizationFromSubjectCommon(spn, subject, lifetime, delegate);
    }

    /**
     * Build a SPNEGO Authorization string using the Native Kerberos credentials of the Operating System
     * account that the Java process is running as.
     * When the WebSphere java process is running on a Windows system under a userid which has Kerberos credentials,
     * the Windows OS maintains a Kerberos Ticket Granting Ticket (TGT) for that user and
     * will use that TGT to request a SPNEGO token can be requested for a ServicePrincipalName (SPN) for the target service system.
     *
     * @param spn      - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - Whether the token includes delegatable GSSCredential credentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException              - thrown when SPNEGO token generation fails, or when SPN is invalid.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     */
    public static String buildSpnegoAuthorizationFromNativeCreds(final String spn, final int lifetime,
                                                                 final boolean delegate) throws GSSException, PrivilegedActionException {
        Krb5Helper.checkSpn(spn);
        String token;
        try {
            token = (String) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws PrivilegedActionException, GSSException {
                    String token = null;
                    String savedProperty = Krb5Helper.setPropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, "false");

                    try {
                        GSSCredential gssCred = Krb5Helper.getGSSCred(null, null, Krb5Common.SPNEGO_MECH_OID, GSSCredential.INITIATE_ONLY, GSSCredential.INDEFINITE_LIFETIME,
                                                                      GSSCredential.INDEFINITE_LIFETIME);
                        token = Krb5Helper.buildSpnegoAuthorization(gssCred, spn, lifetime, delegate);
                    } finally {
                        Krb5Common.restorePropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, savedProperty, "false");
                    }
                    return token;
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable general = Krb5Helper.getGeneralCause(e);
            if (general instanceof GSSException) {
                throw (GSSException) general;
            }
            throw e;
        }

        return token;
    }

    /**
     * Build a SPNEGO Authorization string using a key for a UserPrincipalName from the Kerberos cache
     * or from a key in the Kerberos keytab file used by the process.
     *
     * On a system where a user has logged in, typically using tools such as Java kinit tool,
     * the user's Kerberos credentials are stored in a cache file named krb5cc_userid.
     * Alternatively, a keytab file containing a user's key can be created using a number of tools
     * such as Microsoft's ktpass tool, or the Java ktab tool. These files contain a copy of the user's
     * Kerberos key which can be used to get a Ticket Granting Ticket (TGT) for that userid.
     * The method will use that TGT to request a SPNEGO token can be requested for a ServicePrincipalName (SPN) for the target service system.
     *
     * @param spn                   - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param upn                   - UserPrincipalName of the user for which the SPNEGO token will be generated.
     * @param jaasLoginContextEntry - JAAS login context entry to use.
     * @param lifetime              - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate              - whether the token includes delegatable GSSCredential credentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException              - thrown when SPNEGO token generation fails, when UPN is invalid, or when SPN is invalid.
     * @throws LoginException            - thrown when the Login fails with the supplied UPN.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     */
    public static String buildSpnegoAuthorizationFromUpn(final String spn, final String upn, final String jaasLoginContextEntry,
                                                         final int lifetime, final boolean delegate) throws GSSException, LoginException, PrivilegedActionException {

        Krb5Helper.checkSpn(spn);
        Krb5Helper.checkUpn(upn);

        String token;
        try {
            token = (String) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws LoginException, PrivilegedActionException, GSSException {
                    String token = null;
                    String savedProperty = Krb5Helper.setPropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, "false");
                    try {
                        Subject subject = doKerberosLogin(jaasLoginContextEntry, upn, null);

                        GSSCredential gssCred = (GSSCredential) Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                            @Override
                            public Object run() throws GSSException, Exception {
                                GSSCredential innerCred = Krb5Helper.getGSSCred(null, upn, Krb5Common.SPNEGO_MECH_OID, GSSCredential.INITIATE_ONLY,
                                                                                GSSCredential.INDEFINITE_LIFETIME,
                                                                                GSSCredential.INDEFINITE_LIFETIME);
                                return innerCred;
                            }
                        });
                        token = Krb5Helper.buildSpnegoAuthorization(gssCred, spn, lifetime, delegate);
                    } finally {
                        Krb5Common.restorePropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, savedProperty, "false");
                    }
                    return token;
                }

            });
        } catch (PrivilegedActionException e) {
            Throwable general = Krb5Helper.getGeneralCause(e);
            if (general instanceof LoginException) {
                throw (LoginException) general;
            } else if (general instanceof GSSException) {
                throw (GSSException) general;
            }
            throw e;
        }

        return token;
    }

    /**
     * Build a SPNEGO Authorization string using Kerberos credentials for a user based on userid and password
     * provided. The method will login to the Kerberos Key Distribution (KDC) with the supplied userid and password
     * to get a Ticket Granting Ticket (TGT) then will request the SPNEGO token with that TGT for the specified
     * ServicePrincipalName.
     *
     * @param spn      - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param userid   - Userid for the Login
     * @param password - Password for the Login
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - whether the token includes delegatable GSScredentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException   - thrown when SPNEGO token generation fails, when userid or password is null, or when SPN is invalid.
     * @throws LoginException - thrown when the Login fails with the supplied userid and password.
     */
    public static String buildSpnegoAuthorizationFromUseridPassword(final String spn,
                                                                    final String userid,
                                                                    @Sensitive final String password,
                                                                    final int lifetime,
                                                                    final boolean delegate) throws GSSException, LoginException, PrivilegedActionException {
        return buildSpnegoAuthorizationFromUseridPassword(spn, userid, password, JaasLoginConfigConstants.JAASClient, lifetime, delegate);
    }

    /**
     * Build a SPNEGO Authorization string using Kerberos credentials for a user based on userid and password
     * provided. The method will login to the Kerberos Key Distribution (KDC) with the supplied userid and password
     * to get a Ticket Granting Ticket (TGT) then will request the SPNEGO token with that TGT for the specified
     * ServicePrincipalName.
     *
     * @param spn                   - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param userid                - Userid for the Login
     * @param password              - Password for the Login
     * @param jaasLoginContextEntry - JAAS login context entry to use.
     * @param lifetime              - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate              - whether the token includes delegatable GSSCredentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException              - thrown when SPNEGO token generation fails, when userid or password is null, or when SPN is invalid.
     * @throws LoginException            - thrown when the Login fails with the supplied userid and password.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     */
    public static String buildSpnegoAuthorizationFromUseridPassword(final String spn, final String userid, @Sensitive final String password,
                                                                    final String jaasLoginContextEntry, final int lifetime,
                                                                    final boolean delegate) throws GSSException, LoginException, PrivilegedActionException {

        Krb5Helper.checkSpn(spn);
        Krb5Helper.checkUpn(userid);
        Krb5Helper.checkPassword(password);
        String token;
        try {
            token = (String) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws LoginException, GSSException, PrivilegedActionException {
                    String innerToken = null;
                    Subject useSubject = doKerberosLogin(jaasLoginContextEntry, userid, password);
                    GSSCredential gssCred = (GSSCredential) Subject.doAs(useSubject, new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws GSSException, Exception {
                            GSSCredential innerCred = Krb5Helper.getGSSCred(null, userid, Krb5Common.SPNEGO_MECH_OID, GSSCredential.INITIATE_ONLY,
                                                                            GSSCredential.INDEFINITE_LIFETIME,
                                                                            GSSCredential.INDEFINITE_LIFETIME);

                            return innerCred;
                        }
                    });
                    innerToken = Krb5Helper.buildSpnegoAuthorization(gssCred, spn, lifetime, delegate);

                    return innerToken;
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable general = Krb5Helper.getGeneralCause(e);
            if (general instanceof LoginException) {
                throw (LoginException) general;
            } else if (general instanceof GSSException) {
                throw (GSSException) general;
            }
            throw e;
        }

        return token;
    }

    private static Subject doKerberosLogin(String jaasLoginContextEntry, final String upn, @Sensitive final String password) throws LoginException {
        if (Krb5Common.isIBMJdk18)
            return doKerberosLoginIBMJdk8(jaasLoginContextEntry, upn, password);
        else if (Krb5Common.isOtherSupportJDKs)
            return doKerberosLoginJdk11Up(jaasLoginContextEntry, upn, password);
        else
            return null;
    }

    /**
     * This method authenticate user with the KerberosLoginModule.
     * If the password is null, it will use the Kerberos credential cache or keytab.
     *
     * @param upn
     * @param password
     * @param jaasLoginContextEntry
     * @return
     * @throws LoginException
     */
    private static Subject doKerberosLoginIBMJdk8(String jaasLoginContextEntry, final String upn, @Sensitive final String password) throws LoginException {
        Subject subject = null;
        if (jaasLoginContextEntry == null) {
            jaasLoginContextEntry = JaasLoginConfigConstants.JAASClient;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "jaasLoginContextEntry: " + jaasLoginContextEntry);
        }
        final String inJaasLoginContextEntry = jaasLoginContextEntry;
        try {
            subject = AccessController.doPrivileged(new PrivilegedExceptionAction<Subject>() {
                @Override
                public Subject run() throws LoginException {
                    WSCallbackHandlerImpl callbackHandler = new WSCallbackHandlerImpl(upn, password);
                    LoginContext lc = new LoginContext(inJaasLoginContextEntry, callbackHandler);
                    lc.login();
                    return lc.getSubject();
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable general = Krb5Helper.getGeneralCause(e);
            if (general instanceof LoginException) {
                throw (LoginException) general;
            }
        }
        return subject;
    }

    static Subject doKerberosLoginJdk11Up(String jaasLoginContextEntry, final String upn, @Sensitive final String password) throws LoginException {

        Subject subject = new Subject();

        Krb5LoginModuleWrapper krb5 = new Krb5LoginModuleWrapper();
        Map<String, String> options = new HashMap<String, String>();
        Map<String, Object> sharedState = new HashMap<String, Object>();
        options.put("isInitiator", "true");
        options.put("refreshKrb5Config", "true");
        options.put("doNotPrompt", "false");
        options.put("clearPass", "true");
        options.put("storeKey", "true");
        if (password != null) {
            options.put("useFirstPass", "true");
            sharedState.put(Krb5Common.KRB5_PWD, password.toCharArray());
        } else {
            options.put("tryFirstPass", "true");
            options.put("useTicketCache", "true");
        }
        if (tc.isDebugEnabled()) {
            options.put("debug", "true");
        }
        options.put("principal", upn);
        sharedState.put(Krb5Common.KRB5_NAME, upn);

        WSCallbackHandlerImpl wscbh = new WSCallbackHandlerImpl(upn, password);
        String oldValue = Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, upn);
        krb5.initialize(subject, wscbh, sharedState, options);
        Krb5Common.debugKrb5LoginModule(subject, wscbh, sharedState, options);
        krb5.login();
        krb5.commit();
        Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, oldValue);
        return subject;
    }
}
