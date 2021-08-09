/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20;

import java.io.IOException;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

/**
 *
 */
public interface OpenidClientAuthenticator {

    public void initialize(OpenidClientConfig openidClientConfig, SSLContext sslContext);

    public void createAuthRequest(ServletRequest request, ServletResponse response) throws ServletException, Exception;

    public ProviderAuthenticationResult verifyResponse(ServletRequest request) throws ServletException, IOException;

}
