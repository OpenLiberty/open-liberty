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
package com.ibm.ws.security.jwt.sso.token.utils;

import javax.security.auth.Subject;

import com.ibm.websphere.security.WSSecurityException;

/**
 *
 */
public interface JwtSSOToken {
    void createJwtSSOToken(Subject subject) throws WSSecurityException;

    String getJwtSSOToken(Subject subject);

    Subject handleJwtSSOToken(String encodedjwt);

}
