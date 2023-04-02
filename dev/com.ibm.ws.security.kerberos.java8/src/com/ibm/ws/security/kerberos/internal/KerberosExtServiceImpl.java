/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.kerberos.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.security.kerberos.IBMKrb5Helper;
import com.ibm.ws.security.kerberos.Krb5HelperJdk;
import com.ibm.ws.security.kerberos.OtherKrb5Helper;
import com.ibm.ws.security.kerberos.auth.Krb5LoginModuleWrapper;
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
    private volatile Krb5HelperJdk krb5HelperJdk = null;
    private ServiceRegistration<Krb5HelperJdk> krb5HelperJdkReg;

    /**
     * We don't do anything with the process, but having it set allows us to only be activated by DS if criteria we set
     * about the Java version are met.
     */
    @Reference(policy = ReferencePolicy.STATIC, target = "(java.specification.version>=1.8)")
    protected void setProcess(LibertyProcess process) {}

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        kerberosExtConfig = new KerberosExtConfig(props);
        registerKrb5HelperJdk(cc);
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        kerberosExtConfig = new KerberosExtConfig(props);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (krb5HelperJdkReg != null) {
            krb5HelperJdkReg.unregister();
        }
    }

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

    /**
     * Register the webcontainer's default delegation provider.
     *
     * @param cc
     */
    private void registerKrb5HelperJdk(ComponentContext cc) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        if (Krb5LoginModuleWrapper.IBM_KRB5_LOGIN_MODULE_AVAILABLE) {
            krb5HelperJdk = new IBMKrb5Helper();
            props.put("name", "IBMKrb5Helper");
        } else {
            krb5HelperJdk = new OtherKrb5Helper();
            props.put("name", "OtherKrb5Helper");
        }
        BundleContext bc = cc.getBundleContext();
        krb5HelperJdkReg = bc.registerService(Krb5HelperJdk.class, krb5HelperJdk, props);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Register the OSGI service  " + props.get("name"));
        }
    }
}
