/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.CookieHelper;

import io.openliberty.security.oidcclientcore.authentication.AuthenticationResponseValidator;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;

public class OidcAuthenticationResponseValidator extends AuthenticationResponseValidator {

    public OidcAuthenticationResponseValidator(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /** {@inheritDoc} */
    @Override
    public void validateResponse() throws AuthenticationResponseException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getStoredStateValue(String responseState) throws AuthenticationResponseException {
        javax.servlet.http.Cookie[] cookies = request.getCookies();

        String cookieName = OidcStorageUtils.getStateStorageKey(responseState);
        String stateCookieValue = CookieHelper.getCookieValue(cookies, cookieName); // this could be null if used
        OidcClientUtil.invalidateReferrerURLCookie(request, response, cookieName);
        return stateCookieValue;
    }

}
