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
package com.ibm.websphere.security.s4u2proxy;

import java.security.PrivilegedActionException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.kerberos.Krb5HelperJdk;
import com.ibm.ws.security.kerberos.internal.BoundedHashMap;
import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.s4u2proxy.KerberosExtService;
import com.ibm.ws.security.s4u2proxy.TraceConstants;
import com.ibm.ws.security.token.krb5.Krb5Helper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * SpnegoHelper
 * - utilities to help create a SPNEGO Token as Authorization header for outbound authentication purposes using the Kerberos S4U2self and/or S4U2proxy.
 *
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-api
 *
 */
@Component(service = SpnegoHelper.class,
           name = "SpnegoHelper",
           configurationPid = "com.ibm.websphere.security.s4u2proxy.SpnegoHelper",
           immediate = true,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = "service.vendor=IBM")
public class SpnegoHelper {
    private static final TraceComponent tc = Tr.register(SpnegoHelper.class, TraceConstants.GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static Map<String, Object> delegateSubjectCache = null;
    private static final int MAX_CACHE = 10;
    static final String KEY_KERBEROS_EXT_SERVICE = "KerberosExtService";
    static final String KEY_KRB5_HELPER_JDK = "Krb5HelperJdk";
    private static Krb5HelperJdk krb5HelperJdk = null;
    protected final static AtomicServiceReference<KerberosExtService> kerberosExtServiceRef = new AtomicServiceReference<KerberosExtService>(KEY_KERBEROS_EXT_SERVICE);

    /**
     * Build a SPNEGO Authorization string using a Kerberos credential from the delegate service that impersonates the user (S4U2self).
     * The method will use that credential to request a SPNEGO token for a targetServiceSpn (SPN) for the target service system.
     * If the system property java.security.krb5.conf has not been set by the run time, you need to set the java.security.krb5.conf that point to your Kerberos configuration file.
     *
     * @param upn - UserPrincipalName of the user for which the SPNEGO token will be generated.
     * @param targetServiceSpn - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - Whether the token includes delegatable GSSCredential credentials.
     * @param delegateServiceSpn - Delegate servicePrincipalName of system for which the user already authenticated.
     * @param jaasLoginContextEntry - JAAS login context entry to use.
     * @param krb5Keytab - Kerberos keytab that contained the firstServiceSpn and its keys.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException - thrown when SPNEGO token generation fails, when delegate service's subject is null, when the delegate serivce's subject
     *             does not contain Kerberos credentials, when firstServiceSpn or targetServiceSpn is invalid.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     * @throws LoginException - thrown when the Login fails with the delegate service SPN
     */

    public static String buildS4U2ProxyAuthorizationUsingS4U2Self(final String upn, final String targetServiceSpn,
                                                                  final int lifetime, final boolean delegate,
                                                                  final String delegateServiceSpn, String jaasLoginContextEntry,
                                                                  String krb5Keytab) throws GSSException, PrivilegedActionException, LoginException {

        Krb5Helper.checkUpn(upn);
        Krb5Helper.checkSpn(targetServiceSpn);
        KerberosExtService kerberosExtService = getKerberosExtService();
        if (!kerberosExtService.isS4U2selfEnable()) {
            String methodName = "buildS4U2proxyAuthorization()";
            String msg = TraceNLS.getFormattedMessage(SpnegoHelper.class,
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "KRB_S4U2SELF_IS_NOT_ENABLED",
                                                      new Object[] { methodName },
                                                      "CWWKS4342E: Can not process method {0} because the constrained delegation S4U2self is not enabled.");
            Tr.error(tc, msg);
            throw new GSSException(GSSException.UNAVAILABLE, GSSException.UNAVAILABLE, msg);

        }
        Subject delegateServiceSubject = getDelegateServiceSubject(delegateServiceSpn, jaasLoginContextEntry, krb5Keytab);

        GSSCredential upnGSSCredS4U2self = kerberosExtService.getDelegateGSSCredUsingS4U2self(upn, targetServiceSpn,
                                                                                              GSSName.NT_USER_NAME, GSSCredential.INITIATE_ONLY,
                                                                                              delegateServiceSpn, delegateServiceSubject);
        return Krb5Helper.buildSpnegoAuthorization(upnGSSCredS4U2self, targetServiceSpn, lifetime, delegate);
    }

    /**
     * Build a SPNEGO Authorization string using a Kerberos credential within the supplied Java Subject.
     * The method will use that credential to request a SPNEGO token for a ServicePrincipalName (SPN) for the target service system.
     *
     * @param spn - ServicePrincipalName of system for which SPNEGO token will be targeted.
     * @param subject - Subject containing Kerberos credentials
     * @param lifetime - Lifetime for the context, for example GSSCredential.INDEFINITE_LIFETIME
     * @param delegate - Whether the token includes delegatable GSSCredentials.
     * @return - String "Negotiate " + Base64 encoded version of SPNEGO Token
     * @throws GSSException - thrown when SPNEGO token generation fails, when Subject is null, when the Subject
     *             does not contain Kerberos credentials, or when SPN is invalid.
     * @throws PrivilegedActionException - unexpected - thrown when Java 2 security is misconfigured.
     * @throws LoginException - thrown when the Login fails with the delegate service SPN
     */
    public static String buildS4U2proxyAuthorization(final String spn, final Subject subject, final int lifetime,
                                                     final boolean delegate) throws GSSException, PrivilegedActionException {
        Krb5Helper.checkSpn(spn);
        KerberosExtService kerberosExtService = getKerberosExtService();
        if (!kerberosExtService.isS4U2proxyEnable()) {
            String methodName = "buildS4U2proxyAuthorization()";
            String msg = TraceNLS.getFormattedMessage(SpnegoHelper.class,
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "KRB_S4U2PROXY_IS_NOT_ENABLED",
                                                      new Object[] { methodName },
                                                      "CWWKS4343E: Can not process method {0} because the constrained delegation S4U2proxy is not enabled.");
            Tr.error(tc, msg);
            throw new GSSException(GSSException.UNAVAILABLE, GSSException.UNAVAILABLE, msg);
        }

        return Krb5Helper.buildSpnegoAuthorizationFromSubjectCommon(spn, subject, lifetime, delegate);
    }

    /**
     * This method will authenticate the delegate service spn,
     *
     * @param delegateSpn
     * @param jaasLoginContextEntry
     * @param krb5Keytab
     * @return
     * @throws LoginException
     */
    private static Subject getDelegateServiceSubject(String delegateSpn, final String jaasLoginContextEntry, String krb5Keytab) throws LoginException {
        Subject delegateServiceSubject = (Subject) delegateSubjectCache.get(delegateSpn);
        if (delegateServiceSubject != null) {
            boolean valid = SubjectHelper.isTGTInSubjectValid(delegateServiceSubject, delegateSpn);
            if (valid) {
                return delegateServiceSubject;
            } else {
                delegateSubjectCache.remove(delegateSpn);
            }
        }
        String savedSubjectCredProp = Krb5Common.setPropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, "false");
        String savedKeytabProp = Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_KTNAME, krb5Keytab);
        String savedPrincProp = Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, delegateSpn);
        try {
            if (krb5HelperJdk != null)
                delegateServiceSubject = krb5HelperJdk.doKerberosLogin(jaasLoginContextEntry, delegateSpn, krb5Keytab);
        } finally {
            Krb5Common.restorePropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, savedSubjectCredProp, "false");
            Krb5Common.restorePropertyAsNeeded(Krb5Common.KRB5_KTNAME, savedKeytabProp, krb5Keytab);
            Krb5Common.restorePropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, savedPrincProp, delegateSpn);
        }

        if (delegateServiceSubject != null) {
            delegateSubjectCache.put(delegateSpn, delegateServiceSubject);
        }

        return delegateServiceSubject;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setSystemProperty(final String propName, final String propValue) {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                return System.setProperty(propName, propValue);
            }
        });
    }

    /**
     * @return
     * @throws GSSException
     */
    private static KerberosExtService getKerberosExtService() throws GSSException {
        if (delegateSubjectCache == null) {
            delegateSubjectCache = new BoundedHashMap(MAX_CACHE);
        }

        KerberosExtService kerberosExtService = kerberosExtServiceRef.getService();
        if (kerberosExtService == null) {
            Krb5Helper.serviceNotAvailableException();
        }

        return kerberosExtService;
    }

    @Reference(service = Krb5HelperJdk.class,
               name = KEY_KRB5_HELPER_JDK,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setKrb5HelperJdk(Krb5HelperJdk krb5HelperJdk) {
        this.krb5HelperJdk = krb5HelperJdk;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "The Krb5HelperJdk service with class name " + this.krb5HelperJdk.getClass().getSimpleName() + " has been activated");
    }

    protected void unsetKrb5HelperJdk(Krb5HelperJdk krb5HelperJdk) {
        if (this.krb5HelperJdk == krb5HelperJdk) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The Krb5HelperJdk service with class name " + this.krb5HelperJdk.getClass().getSimpleName() + " has been deactivated");
            this.krb5HelperJdk = null;
        }
    }

    @Reference(service = KerberosExtService.class,
               name = KEY_KERBEROS_EXT_SERVICE,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.STATIC)

    protected void setKerberosExtService(ServiceReference<KerberosExtService> ref) {
        kerberosExtServiceRef.setReference(ref);
    }

    protected void unsetKerberosExtService(ServiceReference<KerberosExtService> ref) {
        kerberosExtServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        kerberosExtServiceRef.activate(cc);
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        kerberosExtServiceRef.deactivate(cc);
    }
}
