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
package com.ibm.ws.security.jwtsso.config;

import java.util.List;

import com.ibm.ws.security.jwt.config.JwtConfig;

/**
 * see metatype.xml and metatype.properties for documentation
 *
 */
public interface JwtSsoBuilderConfig extends JwtConfig {

	boolean isHttpOnlyCookies();

	boolean isSsoUseDomainFromURL();

	List<String> getSsoDomainNames();

	boolean isSetCookiePathToWebAppContextPath();

	boolean isIncludeLtpaCookie();

	boolean isUseLtpaIfJwtAbsent();

	boolean isCookieSecured();

	String getJwtBuilderRef();

	// String getJwtConsumerRef();

	String getCookieName();

}
