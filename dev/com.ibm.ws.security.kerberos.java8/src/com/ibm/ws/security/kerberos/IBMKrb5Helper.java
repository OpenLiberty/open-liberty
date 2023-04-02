/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.kerberos;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.ibm.security.auth.module.Krb5LoginModule;
import com.ibm.security.jgss.ExtendedGSSContext;
import com.ibm.security.jgss.ExtendedGSSCredential;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.krb5.Krb5Common;

/**
 * Handle Kerberos constrained delegation and Krb5LoginModule specific to the IBM JDK
 */
public class IBMKrb5Helper implements Krb5HelperJdk {

    private static final TraceComponent tc = Tr.register(IBMKrb5Helper.class);

    @Override
    public GSSCredential getDelegateGSSCredUsingS4U2self(final String upn,
                                                         String targetServiceSpn,
                                                         final Oid gssNameType,
                                                         final int gssCredUsage,
                                                         final String delegateServiceSpn,
                                                         final Subject delegateServiceSubject) throws GSSException {
        final GSSManager gssManager = GSSManager.getInstance();

        PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                GSSCredential impersonateGssCred = null;
                try {
                    GSSCredential self = gssManager.createCredential(gssCredUsage);
                    GSSName gssName = gssManager.createName(upn, gssNameType);
                    impersonateGssCred = ((ExtendedGSSCredential) self).impersonate(gssName);
                } catch (GSSException e) {
                    Tr.error(tc, "KRB_IMPERSONATE_USER_TO_GET_GSSCRED_FOR_SELF_FAILURE", new Object[] { upn, delegateServiceSpn, e.getMessage() });
                }
                return impersonateGssCred;
            }
        };

        GSSCredential gssCred = (GSSCredential) WSSubject.doAs(delegateServiceSubject, action);
        if (gssCred == null) {
            throw new GSSException(GSSException.FAILURE, GSSException.NO_CONTEXT, "GSSCredential is null");
        }

        return gssCred;
    }

    @Override
    public GSSCredential getDelegateGSSCredUsingS4U2proxy(final String userPrincipalName,
                                                          final GSSContext delegateGSSContext,
                                                          final String delegateServiceSpn) throws GSSException {
        GSSCredential gssCred = null;
        try {
            ExtendedGSSContext extGssContext = (ExtendedGSSContext) delegateGSSContext;
            gssCred = extGssContext.getDelegCred();
        } catch (GSSException e) {
            Tr.error(tc, "KRB_IMPERSONATE_USER_TO_GET_GSSCRED_FOR_BACKEND_SERVICE_FAILURE", new Object[] { userPrincipalName, delegateServiceSpn, e.getMessage() });
        }

        if (gssCred == null) {
            throw new GSSException(GSSException.FAILURE, GSSException.NO_CRED, "GSSCredential is null");
        }

        return gssCred;
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
    @Override
    public Subject doKerberosLogin(String jaasLoginContextEntry, final String delegateSpn, final String krb5Keytab) throws LoginException {
        Subject subject = new Subject();
        Krb5LoginModule krb5 = new Krb5LoginModule();
        Map<String, String> options = new HashMap<String, String>();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        options.put("credsType", "both");
        options.put("useDefaultCcache", "false");
        options.put("forwardable", "true");
        options.put("principal", delegateSpn);
        options.put("useKeytab", krb5Keytab);
        if (tc.isDebugEnabled()) {
            options.put("debug", "true");
        }
        krb5.initialize(subject, null, sharedState, options);
        Krb5Common.debugKrb5LoginModule(subject, null, sharedState, options);
        krb5.login();
        krb5.commit();

        return subject;
    }
}
