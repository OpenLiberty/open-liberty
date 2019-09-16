/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.api;

public interface OauthConsentStore {

    void addConsent(String clientId, String user, String scopeAsString, String resource, String providerId, int lifetimeInSeconds);

    boolean validateConsent(String clientId, String user, String providerId, String[] scopes, String resource);

    void initialize();

    void stopCleanupThread();

}
