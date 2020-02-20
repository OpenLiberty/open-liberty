/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.spnego;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialException;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialProvider;

@Component(service = CredentialProvider.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "type=SPNEGO" })
public class GSSCredentialProvider implements CredentialProvider {
    private static final TraceComponent tc = Tr.register(GSSCredentialProvider.class);

    @Activate
    protected void activate() {}

    @Deactivate
    protected void deactivate() {}

    @Override
    public void setCredential(Subject subject) throws CredentialException {}

    @Override
    public boolean isSubjectValid(Subject subject) {
        GSSCredential gssCredential = SubjectHelper.getGSSCredentialFromSubject(subject);
        try {
            if (gssCredential == null || gssCredential.getRemainingLifetime() > 0) {
                return true;
            }
        } catch (GSSException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem getting the gssCrendential remaining life time.", e);
            }
        }
        return false;
    }
}
