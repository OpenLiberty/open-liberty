/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.granttype;

import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;

public interface OAuth20GrantTypeHandlerFactory {

    public OAuth20GrantTypeHandler getHandler(String providerId, String grantType, OAuth20ConfigProvider config) throws OAuthException;

}
