/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import java.security.Principal;

import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.identitystore.CredentialValidationResult;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class Utils {

    private static final TraceComponent tc = Tr.register(Utils.class);

    public static boolean validateResult(CredentialValidationResult result) throws AuthenticationException {
        Principal principal = result.getCallerPrincipal();
        String username = null;
        if (principal != null) {
            username = principal.getName();
        }
        if (username == null) {
            Tr.error(tc, "JAVAEESEC_CDI_ERROR_USERNAME_NULL");
            String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_USERNAME_NULL");
            throw new AuthenticationException(msg);
        }
        if (result.getCallerUniqueId() == null) {
            Tr.error(tc, "JAVAEESEC_CDI_ERROR_UNIQUE_ID_NULL");
            String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_UNIQUE_ID_NULL");
            throw new AuthenticationException(msg);
        }
        return true;
    }
}
