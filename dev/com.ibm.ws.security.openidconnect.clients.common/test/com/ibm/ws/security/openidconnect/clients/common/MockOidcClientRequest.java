/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public class MockOidcClientRequest extends OidcClientRequest {

    MockOidcClientRequest() {
        // for FAT, make its scope package only
        super();
    };

    public MockOidcClientRequest(ReferrerURLCookieHandler referrerURLCookieHandler) {
        // for FAT, make its scope package only
        super();
        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
    };

    public MockOidcClientRequest(ConvergedClientConfig oidcClientConfig, ReferrerURLCookieHandler referrerURLCookieHandler) {
        // for FAT, make its scope package only
        super();
        this.oidcClientConfig = oidcClientConfig;
        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);
    };

}
