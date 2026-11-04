/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging.saf.internal;

import javax.security.auth.Subject;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.zos.request.logging.ZosRequestLoggingSafService;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * ZosRequestLoggingSafService implementation.
 *
 */
@Component(name = "com.ibm.ws.zos.request.logging.saf", configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", service = { com.ibm.ws.zos.request.logging.ZosRequestLoggingSafService.class })
public class ZosRequestLoggingSafServiceImpl implements ZosRequestLoggingSafService {

    /** {@inheritDoc} */
    @Override
    public String getMappedUserName() {
        String mvsUserId = null;
        Subject subject = null;
        try {
            /* Get the subject */
            if ((subject = WSSubject.getRunAsSubject()) == null) {
                subject = WSSubject.getCallerSubject();
            }
        } catch (WSSecurityException e) {
            // nothing other than the FFDC
        }
        if (subject != null) {
            SAFCredential safCred = getSAFCredential(subject);
            if (safCred != null) {

                if (safCred.getType() == SAFCredential.Type.MAPPED) {
                    mvsUserId = safCred.getMvsUserId();
                }
            }
        }
        return mvsUserId;
    }

    /**
     * Returns the SAFCredential for the given subject.
     *
     * @param subject Subject to use.
     *
     * @return The SAFCredential. Null if there is none.
     */

    protected SAFCredential getSAFCredential(Subject subject) {
        for (SAFCredential safCred : subject.getPrivateCredentials(SAFCredential.class)) {
            return safCred; // Just return the first one (there should only be one).
        }
        return null;
    }

}
