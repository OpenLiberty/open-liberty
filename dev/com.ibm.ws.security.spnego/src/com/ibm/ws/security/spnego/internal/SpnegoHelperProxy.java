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
package com.ibm.ws.security.spnego.internal;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
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

import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.s4u2proxy.KerberosExtService;
import com.ibm.ws.security.token.krb5.Krb5Helper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * SpnegoHelperProxy
 * - utilities to get the S4U2proxy GSSCredential from the Kerberos extentions service
 *
 */
@Component(service = SpnegoHelperProxy.class,
           name = "SpnegoHelperProxy",
           configurationPid = "com.ibm.ws.security.spnego.internal.SpnegoHelperProxy",
           immediate = true,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = "service.vendor=IBM")
public class SpnegoHelperProxy {
    private static Map<String, Object> delegateSubjectCache = null;
    private static final int MAX_CACHE = 10;
    static final String KEY_KERBEROS_EXT_SERVICE = "KerberosExtService";
    static boolean supportJDK = false;
    protected final static AtomicServiceReference<KerberosExtService> kerberosExtServiceRef = new AtomicServiceReference<KerberosExtService>(KEY_KERBEROS_EXT_SERVICE);

    /**
     * Get the GSSCredential from the subject using the S4u2proxy
     * Note: caller already checked s4U2proxyEnabled is true before calling this method
     *
     * @param userPrincipalName - User principal name
     * @param delegateGSSContext - DelegateService GSSContext that is already established with the user.
     * @param delegateServiceSpn - Delegate service SPN that you to establish with the user.
     * @return - GSSCredential
     * @throws GSSException - thrown GSSException when userPrinciplan is null, when delegateServiceSpn is invalid or GSSCredential is null
     */
    public static GSSCredential getDelegateGSSCredUsingS4U2proxy(String userPrincipalName,
                                                                 GSSContext delegateGSSContext,
                                                                 String delegateServiceSpn) throws GSSException {
        Krb5Helper.checkUpn(userPrincipalName);
        KerberosExtService kerberosExtService = getKerberosExtService();

        return kerberosExtService.getDelegateGSSCredUsingS4U2proxy(userPrincipalName, delegateGSSContext, delegateServiceSpn);
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
    public static Subject doKerberosLogin(String delegateSpn, String krb5Keytab) throws LoginException, GSSException {
        KerberosExtService kerberosExtService = getKerberosExtService();
        return kerberosExtService.doKerberosLogin(delegateSpn, krb5Keytab);
    }

    public static boolean isS4U2proxyEnabled() {
        KerberosExtService kerberosExtService = kerberosExtServiceRef.getService();
        if (kerberosExtService != null) {
            return kerberosExtService.isS4U2proxyEnable();
        }
        return false;
    }

    /**
     * This method get the Kerberos extentions service
     *
     * @return
     * @throws GSSException
     */
    private static KerberosExtService getKerberosExtService() throws GSSException {
        if (delegateSubjectCache == null) {
            delegateSubjectCache = new HashMap<String, Object>(MAX_CACHE);
        }

        KerberosExtService kerberosExtService = kerberosExtServiceRef.getService();
        if (kerberosExtService == null) {
            Krb5Helper.serviceNotAvailableException();
        }

        Krb5Helper.unsuportJdkErrorMsg(supportJDK);

        return kerberosExtService;
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
        supportJDK = Krb5Common.isIBMJdk18 || Krb5Common.isOtherSupportJDKs;
        if (supportJDK) {
            kerberosExtServiceRef.activate(cc);
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        kerberosExtServiceRef.deactivate(cc);
        supportJDK = false;
    }
}
