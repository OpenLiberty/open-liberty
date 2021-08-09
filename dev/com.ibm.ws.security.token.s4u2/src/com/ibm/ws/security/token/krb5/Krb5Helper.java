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
package com.ibm.ws.security.token.krb5;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.token.internal.TraceConstants;

/**
 *
 */
public class Krb5Helper {
    private static final TraceComponent tc = Tr.register(Krb5Helper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String KEY_KERBEROS_EXT_SERVICE = "KerberosExtService";

    /**
     * @param spn
     * @param subject
     * @param lifetime
     * @param delegate
     * @param token
     * @return
     * @throws GSSException
     * @throws PrivilegedActionException
     */
    public static String buildSpnegoAuthorizationFromSubjectCommon(final String spn, final Subject subject, final int lifetime,
                                                                   final boolean delegate) throws GSSException, PrivilegedActionException {
        String token = null;
        try {
            token = (String) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws GSSException, PrivilegedActionException {
                    GSSCredential gssCred = getGSSCred(subject, null, Krb5Common.SPNEGO_MECH_OID, GSSCredential.INITIATE_ONLY, lifetime, lifetime);
                    return buildSpnegoAuthorization(gssCred, spn, lifetime, delegate);
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable general = getGeneralCause(e);
            if (general instanceof GSSException) {
                throw (GSSException) general;
            }
            throw e;
        }
        return token;
    }

    /**
     * Build a SPNEGO Authorization string from the GSSCredential supplied for the requested ServicePrincipalName
     *
     * @param gssCred - GSSCredential
     * @param targetSpn - ServicePrincipalName of system for which SPNEGO token will be consumed.
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - whether the token includes delegatable GSSCredentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException - thrown when SPNEGO token generation fails, GSSCredential is null, or when SPN is invalid.
     */
    public static String buildSpnegoAuthorization(GSSCredential gssCred, String targetSpn, int lifetime, boolean delegate) throws GSSException {

        GSSContext context = createSpnegoGSSContext(gssCred, targetSpn, lifetime, delegate);

//        byte[] response = null;
        byte[] response = new byte[1024]; //TODO: UTLE - big enough?
        int len = 0;
        byte[] request = context.initSecContext(response, 0, len);
        String token = "Negotiate " + Base64Coder.encode(request);

        context.dispose();

        return token;
    }

    /**
     * @param gssCred
     * @param targetSpn
     * @param lifetime
     * @param delegate
     * @return
     * @throws GSSException
     */
    public static GSSContext createSpnegoGSSContext(GSSCredential gssCred, String targetSpn, int lifetime, boolean delegate) throws GSSException {
        GSSManager manager = GSSManager.getInstance();
        GSSName backEnd = manager.createName(targetSpn, GSSName.NT_USER_NAME);
        backEnd = backEnd.canonicalize(Krb5Common.SPNEGO_MECH_OID);

        GSSContext context = manager.createContext(backEnd, Krb5Common.SPNEGO_MECH_OID, gssCred, lifetime);
        context.requestMutualAuth(false); // Only Mutual Auth needs to be set.
        context.requestCredDeleg(delegate); // Honor requested delegation mode
        return context;
    }

    /**
     * This method will use the GSSCredential in the subject if the subject is not null. Otherwise, it
     * will create a GSSCredentail for Kerberos mechOid first and then add the SPNEGO credential.
     * This method will add the Mechanism OID GSSCredential if it's not already existed
     *
     * @param useNativeCreds
     * @param gssCred
     * @param mechOid
     * @param usage
     * @param initLifetime
     * @param acceptLifetime
     * @throws GSSException
     */
    public static GSSCredential getGSSCred(Subject subject, String user, Oid mechOid, int usage, int initLifetime, int acceptLifetime) throws GSSException {
        GSSCredential gssCred = null;
        if (subject != null) {
            gssCred = SubjectHelper.getGSSCredentialFromSubject(subject);
        } else {
            gssCred = createKrbMechGSSCred(user);
        }

        addSpnegoMechGSSCred(gssCred, mechOid, usage, initLifetime, acceptLifetime);

        return gssCred;
    }

    /**
     * @param mechOid
     * @param usage
     * @param initLifetime
     * @param acceptLifetime
     * @param gssCred
     * @throws GSSException
     */
    public static void addSpnegoMechGSSCred(GSSCredential gssCred, Oid mechOid, int usage, int initLifetime, int acceptLifetime) throws GSSException {
        if (gssCred != null) {
            Oid[] mechs = gssCred.getMechs();
            // if the mechOid cred is not existed in the gssCred, then add it.
            if (mechOid != null && !mechOid.containedIn(mechs)) {
                GSSName gssName = gssCred.getName();
                gssCred.add(gssName, initLifetime, acceptLifetime, mechOid, usage);
            }
        } else {
            throw new GSSException(GSSException.NO_CRED);
        }
    }

    /**
     * @param user
     * @param gssName
     * @return
     * @throws GSSException
     */
    private static GSSCredential createKrbMechGSSCred(String user) throws GSSException {
        GSSName gssName = null;
        GSSManager manager = GSSManager.getInstance();
        if (user != null) {
            gssName = manager.createName(user, GSSName.NT_USER_NAME, Krb5Common.KRB5_MECH_OID);
        }
        // if user is null, then it uses the Kerberos native cache
        GSSCredential gssCred = manager.createCredential(gssName,
                                                         GSSCredential.INDEFINITE_LIFETIME,
                                                         Krb5Common.KRB5_MECH_OID,
                                                         GSSCredential.INITIATE_ONLY);
        return gssCred;
    }

    /**
     * This method set the system property if the property is null or property value is not the same with the new value
     *
     * @param propName
     * @param propValue
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String setPropertyAsNeeded(final String propName, final String propValue) {

        String savedPropValue = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public String run() {
                String oldValue = System.getProperty(propName);
                if (oldValue == null || !oldValue.equalsIgnoreCase(propValue)) {
                    System.setProperty(propName, propValue);
                }
                return oldValue;
            }
        });
        if (tc.isDebugEnabled())
            Tr.debug(tc, Krb5Common.USE_SUBJECT_CREDS_ONLY + " property previous: " + ((savedPropValue != null) ? savedPropValue : "<null>") + " and now: " + propValue);

        return savedPropValue;
    }

    public static void serviceNotAvailableException() throws GSSException {
        Tr.error(tc, "KRB_OSGI_SERVICE_ERROR");
        String msg = TraceNLS.getFormattedMessage(Krb5Helper.class,
                                                  com.ibm.ws.security.token.internal.TraceConstants.MESSAGE_BUNDLE,
                                                  "KRB_OSGI_SERVICE_ERROR",
                                                  new Object[] { KEY_KERBEROS_EXT_SERVICE },
                                                  "CWWKS4003E: The constrained delegation OSGi service {0} is not available.");
        throw new GSSException(GSSException.UNAVAILABLE, GSSException.UNAVAILABLE, msg);
    }

    public static void unsuportJdkErrorMsg(boolean supportJdk) throws GSSException {
        if (!supportJdk) {
            //TODO: UTLE - change error msg
            Tr.error(tc, "KRB_S4U2PROXY_NOT_SUPPORTED");
            String msg = TraceNLS.getFormattedMessage(Krb5Helper.class,
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "KRB_S4U2PROXY_NOT_SUPPORTED",
                                                      null,
                                                      "CWWKS4002E: The constrained delegation (S4U2self and S4U2proxy) API requires a minimum Java runtime environment version of JavaSE 1.8.");
            throw new GSSException(GSSException.UNAVAILABLE, GSSException.UNAVAILABLE, msg);
        }
    }

    /**
     * Utility method to ensure that password supplied is non empty, and not null
     *
     * @param password - password
     * @throws GSSException - Major NO_CRED - when password is invalid
     */
    public static void checkPassword(@Sensitive String password) throws GSSException {
        if (password == null || "".equals(password)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Empty password supplied");
            throw new GSSException(GSSException.NO_CRED);
        }
    }

    /**
     * Utility method to unravel a PrivilegedActionException to its underlying cause
     */
    public static Throwable getGeneralCause(PrivilegedActionException pae) {
        Throwable retVal = pae;

        if (pae != null) {
            Throwable cause = pae.getCause();
            if (cause != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Deciphering a PrivilegedActionException [" + cause.getClass().getName() + "]");
                }

                while (cause != null && cause instanceof PrivilegedActionException) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Unravelling");
                    cause = cause.getCause();
                }

                if (cause != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Unravelled to a " + cause.getClass().getName());
                    retVal = cause;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Only PrivilegedActionException in stack.  Returning original exception.");
                }
            }
        }
        return retVal;
    }

    /**
     * Utility method to ensure that ServicePrincipalName supplied is non empty, and not null
     *
     * @param spn - ServicePrincipalName
     * @throws GSSException - Major BAD_NAME - when SPN is invalid
     */
    public static void checkSpn(String spn) throws GSSException {
        if (spn == null || "".equals(spn) || !spn.contains("/")) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Empty or invalid format servicePrincipalName supplied");
            throw new GSSException(GSSException.BAD_NAME);
        }
    }

    /**
     * Utility method to ensure that UserPrincipalName supplied is non empty, and not null
     *
     * @param spn - UserPrincipalName
     * @throws GSSException - Major BAD_NAME - when UPN is invalid
     */
    public static void checkUpn(String upn) throws GSSException {
        if (upn == null || "".equals(upn)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Empty UserPrincipalName supplied");
            throw new GSSException(GSSException.BAD_NAME);
        }
    }
}
