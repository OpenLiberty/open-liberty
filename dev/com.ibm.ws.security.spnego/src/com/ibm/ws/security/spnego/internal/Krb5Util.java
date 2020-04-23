/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.internal;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.servlet.http.HttpServletResponse;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.spnego.SpnegoConfig;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 *
 */
public class Krb5Util {
    private static final TraceComponent tc = Tr.register(Krb5Util.class);

    private final static char SPACE = ' ';
    private final static char TAB = '\t';
    private final static char NEWLINE = '\n';
    private final static char TILDA = '~';
    private final static char DOT = '.';
    private static HashMap<String, Subject> delegateSubjectCache = null;
    static boolean ibmJDK = false;

    /**
     * @param resp
     * @param tokenByte
     * @param reqHostName
     * @param spnegoConfig
     * @return
     * @throws AuthenticationException
     */
    public AuthenticationResult processSpnegoToken(HttpServletResponse resp,
                                                   @Sensitive byte[] tokenByte,
                                                   String reqHostName,
                                                   SpnegoConfig spnegoConfig) throws AuthenticationException {
        AuthenticationResult result = null;
        GSSCredential spnGssCred = spnegoConfig.getSpnGSSCredential(reqHostName);
        String spnWithoutRealm = "HTTP/" + reqHostName;
        String currentSpn = null;

        if (spnGssCred == null) {
            Tr.error(tc, "SPNEGO_GSSCRED_NOT_FOUND_FOR_SPN", new Object[] { spnWithoutRealm });
            return new AuthenticationResult(AuthResult.SEND_401, "There is no GSSCredential for SPN");
        } else {
            try {
                currentSpn = spnGssCred.getName().toString();
            } catch (GSSException e) {
                Tr.error(tc, "spnWithouRealm: " + spnWithoutRealm + "currentSpn: " + currentSpn);
            }
        }

        Subject clientSubject = new Subject();
        Subject delegateSubject = new Subject();

        GSSContext gssContext = null;
        String currentUseSubjectCredsOnly = "true";
        String preUseSubjectCredsOnly = null;

        String previousSpn = null;

        if (SpnegoHelperProxy.isS4U2proxyEnabled()) {
            currentUseSubjectCredsOnly = "false";
        }

        try {
            preUseSubjectCredsOnly = Krb5Common.setPropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, currentUseSubjectCredsOnly);
            if (Krb5Common.isOtherSupportJDKs) {
                previousSpn = Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, currentSpn);
                Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_NAME, currentSpn);
            }
            if (SpnegoHelperProxy.isS4U2proxyEnabled()) {
                // if we got any exception, client subject will not have the client delegate service GSSCredential
                // but continue to validate the SPNEGO token
                delegateSubject = getdelegateServiceSubject(spnWithoutRealm, currentSpn, spnegoConfig);
                // clientSubject = getdelegateServiceSubject(spnWithoutRealm, spnGssCred, spnegoConfig);
            }
            gssContext = validateSpnegoToken(resp, spnGssCred, tokenByte, delegateSubject, spnegoConfig);
            if (gssContext.isEstablished()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SPNEGO request token successfully processed. GSSContext is established");
                }
                result = createResult(clientSubject, gssContext, spnegoConfig);
            } else {
                result = new AuthenticationResult(AuthResult.FAILURE, "GSSContext is not established with a valid SPNEGO token");
            }
            disposeGssContext(gssContext);
        } finally {
            Krb5Common.restorePropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, preUseSubjectCredsOnly, currentUseSubjectCredsOnly);
            if (Krb5Common.isOtherSupportJDKs) {
                Krb5Common.restorePropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, previousSpn, currentSpn);
                Krb5Common.restorePropertyAsNeeded(Krb5Common.KRB5_NAME, previousSpn, currentSpn);
            }
        }
        return result;
    }

    public GSSContext validateSpnegoToken(HttpServletResponse resp, GSSCredential spnGssCredential, @Sensitive final byte[] inToken, Subject clientSubject,
                                          SpnegoConfig spnegoConfig) throws AuthenticationException {

        final GSSContext gssContext = createGssContext(spnGssCredential, spnegoConfig);

        byte[] responseToken = null;

        PrivilegedExceptionAction<byte[]> action = new PrivilegedExceptionAction<byte[]>() {
            @Override
            public byte[] run() throws Exception {
                return gssContext.acceptSecContext(inToken, 0, inToken.length);
            }
        };

        try {
            responseToken = Subject.doAsPrivileged(clientSubject, action, java.security.AccessController.getContext());
        } catch (PrivilegedActionException e) {
            try {
                throw e.getException();
            } catch (Exception ee) {
                throw new AuthenticationException(ee.getLocalizedMessage(), ee);
            }
        }

        if (!gssContext.isEstablished()) {
            String responsebytes = Krb5Util.showHex(responseToken);
            Tr.error(tc, "SPNEGO_CAN_NOT_ESTABLISH_GSSCONTEXT_WITH_VALIDATE_TOKEN", new Object[] { responsebytes });
        }

        if (responseToken != null) {
            setServerResponseToken(resp, responseToken);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "client subject: ", (clientSubject != null ? clientSubject : "is null"));
        }

        return gssContext;
    }

    /**
     * @param spnGssCredential
     * @param spnegoConfig
     * @return
     * @throws AuthenticationException
     */
    protected GSSContext createGssContext(GSSCredential spnGssCredential, SpnegoConfig spnegoConfig) throws AuthenticationException {
        GSSContext gssContext = null;
        GSSManager gssManager = GSSManager.getInstance();
        try {
            gssContext = gssManager.createContext(spnGssCredential);
        } catch (GSSException e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        }
        return gssContext;
    }

    /**
     * @param resp
     * @param respToken
     */
    @Trivial
    protected void setServerResponseToken(HttpServletResponse resp, byte[] respToken) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Server response token: " + Krb5Util.showHex(respToken));
        }
        //send back the server response token in the header
        String responseEnc = new String(Base64Coder.base64Encode(respToken));
        resp.setHeader("WWW-Authenticate", "Negotiate " + responseEnc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Encoded response: " + responseEnc);
        }
    }

    /**
     * @param clientSubject
     * @param gssContext
     * @param spnegoConfig
     * @param delegateServiceSpn
     * @return
     * @throws AuthenticationException
     */
    AuthenticationResult createResult(Subject clientSubject, GSSContext gssContext,
                                      SpnegoConfig spnegoConfig) throws AuthenticationException {
        String userPrincipal = null;
        try {
            userPrincipal = gssContext.getSrcName().toString();
        } catch (GSSException e) {
            throw new AuthenticationException(e.getLocalizedMessage(), e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Kerberos client principal: " + userPrincipal);
        }
        KerberosPrincipal krb5Principal = new KerberosPrincipal(userPrincipal);
        clientSubject.getPrincipals().add(krb5Principal);

        String authName = userPrincipal;
        if (spnegoConfig.isTrimKerberosRealmNameFromPrincipal()) {
            authName = trimUsername(userPrincipal);
        }

        String customCacheKey = userPrincipal;
        if (spnegoConfig.isIncludeClientGSSCredentialInSubject()) {
            GSSCredential clientGSSCred = addGSSDelegCredToSubject(userPrincipal, gssContext, clientSubject, spnegoConfig);
            if (clientGSSCred != null) {
                customCacheKey = userPrincipal + clientGSSCred.hashCode();
            }
        }

        addCustomProperties(clientSubject, customCacheKey, authName, spnegoConfig);

        return new AuthenticationResult(AuthResult.SUCCESS, clientSubject);
    }

    /**
     *
     */
    private void addCustomProperties(Subject subject,
                                     String customCacheKey,
                                     String authName,
                                     SpnegoConfig spnegoConfig) {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, authName);
        if (spnegoConfig.isIncludeCustomCacheKeyInSubject()) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
        }
        if (spnegoConfig.isDisableLtpaCookie()) {
            customProperties.put(AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE, Boolean.TRUE);
        }
        subject.getPrivateCredentials().add(customProperties);
    }

    /**
     * @param gssContext
     */
    public void disposeGssContext(GSSContext gssContext) {
        if (gssContext != null) {
            try {
                gssContext.dispose();
            } catch (GSSException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "GSSException: " + e);
                }
            }
        }
    }

    @FFDCIgnore({ GSSException.class })
    private GSSCredential addGSSDelegCredToSubject(String userPrincipal, GSSContext context, Subject subject, SpnegoConfig spnegoConfig) {
        GSSCredential clientCred = null;
        try {
            clientCred = context.getDelegCred();
        } catch (GSSException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "gssClientCredential has not been saved in the subject, GSSException: " + e.getMessage());
            }
        }

        try {
            if (clientCred == null && SpnegoHelperProxy.isS4U2proxyEnabled()) {
                String delegateServiceSpn = context.getTargName().toString();
                clientCred = SpnegoHelperProxy.getDelegateGSSCredUsingS4U2proxy(userPrincipal, context, delegateServiceSpn);
            }

            if (clientCred != null) {
                subject.getPrivateCredentials().add(clientCred);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add gssClientCredential " + userPrincipal + " to subject");
                }
            } else {
                Tr.warning(tc, "SPNEGO_NO_DELEGATED_CREDENTIALS_FOUND", new Object[] { userPrincipal });
            }
        } catch (GSSException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "gssClientCredential has not been saved in the subject, GSSException: " + e.getMessage());
            }
        }
        return clientCred;
    }

    /*
     * This method method will remove the @ and Kerberos realm name from the Kerberos principal name
     */
    public String trimUsername(String principal) {
        if (principal == null || principal.length() == 0)
            return null;
        String p = principal;
        int i = p.lastIndexOf("@");
        if (i != -1) {
            p = p.substring(0, i);
        }
        return p;
    }

    public void setKrb5ConfigAndKeytabProps(SpnegoConfig spnegoConfig) {
        String krb5Keytab = spnegoConfig.getKrb5Keytab();
        if (krb5Keytab != null) {
            Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_KTNAME, krb5Keytab);
        }
        String krb5Config = spnegoConfig.getKrb5Config();

        if (krb5Config != null) {
            Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_CONF, krb5Config);
        }

    }

    /**
     *
     * Method returns a formatted hex representation of the bytes in a buffer
     * array.
     *
     * @param aBuffer
     *            byte[] contains the bytes to convert to hex
     * @param nBytes
     *            int the number of entries in the array to display
     * @return String a string that represents the hex equivalent of the byte
     *         array.
     */
    @Trivial
    public static String showHex(final byte[] buffer) {
        if (buffer == null)
            return null;
        final int nBytes = buffer.length;
        final StringBuffer sb = new StringBuffer(nBytes); // formatted string
        final StringBuffer pb = new StringBuffer(nBytes << 1); // formatted
        // binary string
        final StringBuffer hb = new StringBuffer(nBytes << 1); // hex buffer
        final StringBuffer tb = new StringBuffer(nBytes << 1); // text buffer
        int j = 0; // line position counter
        int k = 0; // byte counter
        float f = 0; // binary character counter

        for (int i = 0; i < nBytes; i++) {
            final int value = buffer[i] & 0xFF; // 32 bit Unicode to 16 bit
            // ASCII
            if (value == '\r' || value == NEWLINE || value == TAB
                || (value >= SPACE && value <= TILDA)) {
                sb.append((char) value); // text character
            } else {
                sb.append('[' + hexPad(Integer.toHexString(value), 2) + ']');
                // binary character
                f++;
            }

            if (value >= SPACE && value <= TILDA) {
                tb.append((char) value); // "printable" character
            } else {
                tb.append(DOT); // non-printable
            }

            hb.append(hexPad(Integer.toHexString(value), 2));
            if (j == 3 || j == 7 || j == 11) { // for readability, space every 4
                // chars
                hb.append(SPACE);
                tb.append(SPACE);
            }

            if (j == 15) { // 16 characters per line
                String pad = hexPad(Integer.toHexString(k), 4);
                pb.append(pad).append(":  ").append(hb).append("    ").append(tb).append("\n");
                j = 0;
                k += 16;
                hb.setLength(0);
                tb.setLength(0);
            } else {
                j++;
            }
        }
        for (int i = hb.length(); i < 35; i++) {
            hb.append(SPACE); // pad to length of other lines
        }
        String pad = hexPad(Integer.toHexString(k), 4);
        pb.append(pad).append(":  ").append(hb).append("    ").append(tb).append("\n");

        return pb.toString();
    }

    /**
     * Prepend 0's to a hex string.
     *
     * @param aString
     *            String is the string that contains the hex characters to which
     *            '0' will be prepended
     * @param aPadLength
     *            int is the number of '0's to prepend to aString
     * @return String aString, now prepended with aPadLength of '0's
     */
    @Trivial
    private static String hexPad(final String aString, final int aPadLength) {
        final int stringSize = aString.length();
        final StringBuffer buffer = new StringBuffer(stringSize + aPadLength);

        for (int i = stringSize; i < aPadLength; i++) {
            buffer.append('0');
        }
        buffer.append(aString);
        return buffer.toString();
    }

    private static Subject getdelegateServiceSubject(String spnWithoutRealm, String delegateServiceSpn, SpnegoConfig spnegoConfig) {

        Subject delegateServiceSubject = getDelegateServiceSubjectFromCache(delegateServiceSpn);
        if (delegateServiceSubject != null) {
            return delegateServiceSubject;
        }

        try {
            delegateServiceSubject = SpnegoHelperProxy.doKerberosLogin(delegateServiceSpn, spnegoConfig.getKrb5Keytab());
        } catch (Exception e) {
            Tr.error(tc, "SPNEGO_DELEGATE_SPN_LOGIN_TO_KDC_FAILURE", new Object[] { spnWithoutRealm, spnegoConfig.getKrb5Config(), spnegoConfig.getKrb5Keytab() });
        }

        if (delegateServiceSubject != null) {
            delegateSubjectCache.put(delegateServiceSpn, delegateServiceSubject);
        }

        return delegateServiceSubject;
    }

    /**
     * @param delegateServiceSpn
     * @return
     */
    private static Subject getDelegateServiceSubjectFromCache(String delegateServiceSpn) {
        if (delegateSubjectCache == null) {
            delegateSubjectCache = new HashMap<String, Subject>();
            return null;
        }

        Subject delegateServiceSubject = delegateSubjectCache.get(delegateServiceSpn);
        if (delegateServiceSubject != null) {
            boolean valid = SubjectHelper.isTGTInSubjectValid(delegateServiceSubject, delegateServiceSpn);
            if (valid) {
                return delegateServiceSubject;
            } else {
                delegateSubjectCache.remove(delegateServiceSpn);
            }
        }

        return delegateServiceSubject;
    }
}
