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
package io.openliberty.security.jakartasec.identitystore;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.context.SubjectManager;

import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

public class OpenIdContextUtils {

    public static OpenIdContext getOpenIdContextFromSubject() {
        Subject sessionSubject = getSessionSubject();
        if (sessionSubject == null) {
            return null;
        }
        Set<OpenIdContext> creds = sessionSubject.getPrivateCredentials(OpenIdContext.class);
        for (OpenIdContext openIdContext : creds) {
            // there should only be one OpenIdContext in the clientSubject.getPrivateCredentials(OpenIdContext.class) set.
            return openIdContext;
        }
        return null;
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private static Subject getSessionSubject() {
        Subject sessionSubject = null;
        try {
            sessionSubject = (Subject) java.security.AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return new SubjectManager().getCallerSubject();
                }
            });
        } catch (PrivilegedActionException pae) {

        }
        return sessionSubject;
    }

}
