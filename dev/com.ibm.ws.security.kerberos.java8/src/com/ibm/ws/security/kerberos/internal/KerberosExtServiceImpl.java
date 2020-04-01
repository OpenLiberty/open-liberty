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
package com.ibm.ws.security.kerberos.internal;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.security.kerberos.Krb5HelperJdk;
import com.ibm.ws.security.s4u2proxy.KerberosExtService;

/**
 * KerberosServiceImpl
 * - Utilities to help impersonation an user
 *
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 *
 */
@Component(service = { KerberosExtService.class },
           name = "KerberosExtService",
           immediate = true,
           configurationPid = "com.ibm.ws.security.s4u2proxy.KerberosExtService",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM" })
public class KerberosExtServiceImpl implements KerberosExtService {
    private static final TraceComponent tc = Tr.register(KerberosExtServiceImpl.class);
    static final String KEY_KRB5_HELPER_JDK = "Krb5HelperJdk";
    private KerberosExtConfig kerberosExtConfig = null;
    private Krb5HelperJdk krb5HelperJdk = null;

    /**
     * We don't do anything with the process, but having it set allows us to only be activated by DS if criteria we set
     * about the Java version are met.
     */
    @Reference(policy = ReferencePolicy.STATIC, target = "(java.specification.version>=1.8)")
    protected void setProcess(LibertyProcess process) {}

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

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        kerberosExtConfig = new KerberosExtConfig(props);
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        kerberosExtConfig = new KerberosExtConfig(props);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    /** {@inheritDoc} */
    @Override
    public GSSCredential getDelegateGSSCredUsingS4U2self(final String upn,
                                                         String targetServiceSpn,
                                                         final Oid gssNameType,
                                                         final int gssCredUsage,
                                                         final String delegateServiceSpn,
                                                         final Subject delegateServiceSubject) throws GSSException {
        if (krb5HelperJdk != null)
            return krb5HelperJdk.getDelegateGSSCredUsingS4U2self(upn,
                                                                 targetServiceSpn,
                                                                 gssNameType,
                                                                 gssCredUsage,
                                                                 delegateServiceSpn,
                                                                 delegateServiceSubject);
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public GSSCredential getDelegateGSSCredUsingS4U2proxy(final String userPrincipalName,
                                                          final GSSContext delegateGSSContext,
                                                          final String delegateServiceSpn) throws GSSException {
        if (krb5HelperJdk != null)
            return krb5HelperJdk.getDelegateGSSCredUsingS4U2proxy(userPrincipalName, delegateGSSContext, delegateServiceSpn);
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public Subject doKerberosLogin(String delegateSpn, String krb5Keytab) throws LoginException {
        if (krb5HelperJdk != null)
            return krb5HelperJdk.doKerberosLogin(null, delegateSpn, krb5Keytab);
        else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isS4U2selfEnable() {
        return (kerberosExtConfig != null ? kerberosExtConfig.isS4U2selfEnable() : false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isS4U2proxyEnable() {
        return (kerberosExtConfig != null ? kerberosExtConfig.isS4U2proxyEnable() : false);
    }
}
