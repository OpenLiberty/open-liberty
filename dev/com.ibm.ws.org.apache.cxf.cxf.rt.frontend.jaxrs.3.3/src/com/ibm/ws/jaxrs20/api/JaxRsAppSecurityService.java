/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.api;

import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.Cookie;

/**
 *
 */
public interface JaxRsAppSecurityService {

    public Cookie getSSOCookieFromSSOToken();

    public SSLSocketFactory getSSLSocketFactory(String sslRef, Map<String, Object> props);
}
