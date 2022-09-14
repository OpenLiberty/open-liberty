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
package io.openliberty.security.oidcclientcore.logout;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.webcontainer.security.CookieHelper;

import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;

public class LocalLogoutStrategy {
    private final HttpServletRequest req;

    public LocalLogoutStrategy(HttpServletRequest req) {
        this.req = req;
    }

    public void logout() throws ServletException {
        if (requestHasStateCookie(req)) {
            return;
        }
        req.logout();
    }

    private static boolean requestHasStateCookie(HttpServletRequest req) {
        return CookieHelper.getCookie(req.getCookies(), OidcClientStorageConstants.WAS_OIDC_STATE_KEY) != null;
    }
}
