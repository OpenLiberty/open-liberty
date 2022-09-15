/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.beans;

import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;

import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.jakartasec.identitystore.OpenIdContextImpl;
import jakarta.enterprise.context.SessionScoped;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

/**
 * Session scoped bean to fetch the openIdContext from the thread. This can be used by the
 *
 * @OpenIdContextProducer to supply @Inject requests for the OpenIdContext.
 */
@SessionScoped
public class OpenIdContextBean implements Serializable {

    private static final long serialVersionUID = 1L; // TODO -- default or custom?? or switch to PassivationCapable instead of Serializable

    private static final TraceComponent tc = Tr.register(OpenIdContextBean.class);

    public OpenIdContext getOpenIdContext() {

        Subject subject = null;
        OpenIdContext openIdContext = null;

        try {
            subject = (Subject) java.security.AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return new SubjectManager().getCallerSubject();
                }
            });
        } catch (PrivilegedActionException pae) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext() Exception caught: " + pae);
            }
        }

        SubjectHelper subjectHelper = new SubjectHelper();

        if (subject != null && !subjectHelper.isUnauthenticated(subject)) {

            Set<OidcTokensCredential> oidcTokensCredentialSet = subject.getPrivateCredentials(OidcTokensCredential.class);

            if (oidcTokensCredentialSet == null || oidcTokensCredentialSet.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getOpenIdContext() Got an authenticated subject, but did not find an OidcTokensCredential");
                }
            } else {
                if (oidcTokensCredentialSet.size() > 1) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getOpenIdContext() Multiple OidcTokensCredentials on the subject!");
                    }
                }
                OidcTokensCredential oidcTokensCredential = oidcTokensCredentialSet.iterator().next();
                // TODO from oidcTokensCredential get or create an OpenIdContext
            }

            // TODO returning an empty OpenIdContext temporarily
            openIdContext = new OpenIdContextImpl();
        } else if (subject == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext The subject is null from SubjectManager.getCallerSubject");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext The subject is not null but is unauthentictaed.");
            }
        }

        if (openIdContext == null) {
            // TODO: do we need to log message or throw an exception here or just trace?

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOpenIdContext() Did not find an openIdContext, returning null");
            }
        }

        /*
         * TODO: Could be returning null here -- do we want to return an empty or dummy openIdContext or stay with null if not found?
         */
        return openIdContext;
    }

}
