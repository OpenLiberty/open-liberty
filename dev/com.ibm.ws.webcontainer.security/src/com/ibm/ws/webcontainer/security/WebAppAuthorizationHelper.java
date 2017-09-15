/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import javax.security.auth.Subject;

import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.webcontainer.security.internal.WebReply;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * Encapsulate jacc related methods which are consumed by WebAppSecurityCollaborator.
 */
public interface WebAppAuthorizationHelper {
    boolean isUserInRole(String role, IExtendedRequest req, Subject subject);

    boolean authorize(AuthenticationResult authResult, WebRequest webRequest, String uriName);

    boolean isSSLRequired(WebRequest webRequest, String uriName);

    WebReply checkPrecludedAccess(WebRequest webRequest, String uriName);

}
