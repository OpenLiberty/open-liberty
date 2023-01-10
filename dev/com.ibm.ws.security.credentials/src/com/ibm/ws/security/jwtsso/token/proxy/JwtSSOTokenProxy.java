/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.jwtsso.token.proxy;

import javax.security.auth.Subject;

import com.ibm.websphere.security.auth.WSLoginFailedException;

/**
 *
 */
public interface JwtSSOTokenProxy {

    void createJwtSSOToken(Subject subject) throws WSLoginFailedException;

    String getJwtSSOToken(Subject subject);

    Subject handleJwtSSOTokenValidation(Subject subject, String encodedjwt) throws WSLoginFailedException;

    String getCustomCacheKeyFromJwtSSOToken(String encodedjwt);

    String getCacheKeyForJwtSSOToken(Subject subject, String encodedjwt);

    void addAttributesToJwtSSOToken(Subject subject) throws WSLoginFailedException;

    boolean isSubjectValid(Subject subject);

    String getJwtCookieName();

    boolean isCookieSecured();

    long getValidTimeInMinutes();

    boolean shouldSetJwtCookiePathToWebAppContext();

    boolean shouldAlsoIncludeLtpaCookie();

    boolean shouldUseLtpaIfJwtAbsent();

    boolean isDisableJwtCookie();

    String getAuthFilterRef();

}
