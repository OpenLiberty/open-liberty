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
package com.ibm.ws.security.sso.common.saml.propagation;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.Saml20Token;

/**
 *
 */
public class SamlCommonUtil {

    protected static final TraceComponent tc = Tr.register(SamlCommonUtil.class,
                                                           TraceConstants.TRACE_GROUP,
                                                           TraceConstants.MESSAGE_BUNDLE);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Saml20Token getSaml20TokenFromSubject(final Subject subject) {
        //our own runtime is accessing this information. So, doPriv here should be okay
        Saml20Token saml20Token = null;
        try {
            saml20Token = (Saml20Token) AccessController.doPrivileged(
                            new PrivilegedExceptionAction() {
                                @Override
                                public Object run() throws Exception
                                {
                                    final Iterator authIterator = subject.getPrivateCredentials(Saml20Token.class).iterator();
                                    if (authIterator.hasNext()) {
                                        final Saml20Token token = (Saml20Token) authIterator.next();
                                        return token;
                                    }
                                    Tr.warning(tc, "no_saml_found_in_subject");
                                    return null;
                                }
                            });
        } catch (PrivilegedActionException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting SAML token from subject:", e.getCause());
            }
            Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
        }
        return saml20Token;
    }

    /**
     * @param subject
     * @param b
     * @return
     */
    public static Saml20Token getSaml20TokenFromSubject(Subject subject, boolean isPrivileged) {
        //this method is trying to get the credentials without doPriv.
        //We will leave it up to the application to have proper java2 access
        if (isPrivileged) {
            return (getSaml20TokenFromSubject(subject));
        }
        Saml20Token saml20Token = null;
        try {
            Iterator authIterator = subject.getPrivateCredentials(Saml20Token.class).iterator();
            if (authIterator.hasNext()) {
                saml20Token = (Saml20Token) authIterator.next();
            }
        } catch (Exception e) {
            Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
        }
        if (saml20Token == null) {
            Tr.warning(tc, "no_saml_found_in_subject");
        }
        return saml20Token;
    }

}
