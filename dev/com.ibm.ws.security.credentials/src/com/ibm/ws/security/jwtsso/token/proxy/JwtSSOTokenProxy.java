/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.token.proxy;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 *
 */
public interface JwtSSOTokenProxy {

    void createJwtSSOToken(Subject subject) throws LoginException;

    String getJwtSSOToken(Subject subject);

    Subject handleJwtSSOTokenValidation(Subject subject, String encodedjwt) throws LoginException;

    String getCustomCacheKeyFromJwtSSOToken(String encodedjwt);

    String getCacheKeyForJwtSSOToken(Subject subject, String encodedjwt);

    void addCustomCacheKeyAndRealmToJwtSSOToken(Subject subject) throws LoginException;

    boolean isSubjectValid(Subject subject);

    String getJwtCookieName();

    boolean isCookieSecured();

    boolean shouldSetJwtCookiePathToWebAppContext();

    boolean shouldAlsoIncludeLtpaCookie();

    boolean shouldFallbackToLtpaCookie();

}
