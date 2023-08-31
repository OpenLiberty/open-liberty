/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security;

import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Single sign-on cookie helper class.
 */
public interface SSOCookieHelper {

    List<Cookie> addSSOCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp, String contextRoot);

    boolean addJwtSsoCookiesToResponse(Subject subject, HttpServletRequest req, HttpServletResponse resp, String contextRoot);

    void createLogoutCookies(HttpServletRequest req, HttpServletResponse resp);

    void createLogoutCookies(HttpServletRequest req, HttpServletResponse resp, boolean deleteJwtCookies);

    SingleSignonToken getDefaultSSOTokenFromSubject(final javax.security.auth.Subject subject);

    String getSSOCookiename();

    void removeSSOCookieFromResponse(HttpServletResponse resp);

    boolean allowToAddCookieToResponse(HttpServletRequest req);

    String getSSODomainName(HttpServletRequest req, List<String> ssoDomainList, boolean useURLDomain);

    String getJwtSsoTokenFromCookies(HttpServletRequest req, String jwtCookieName);
}