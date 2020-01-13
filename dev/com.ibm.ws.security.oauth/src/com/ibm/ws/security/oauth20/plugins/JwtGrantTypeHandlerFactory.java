/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.plugins;

import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;

public interface JwtGrantTypeHandlerFactory {

    /**
     * set the configuration information into the new customized Grant Type Handler Factory
     * 
     * @param customizedGrantTypeHandlerInfo
     */
    public void setHandlerInfo(String providerId, OAuth20Provider config);

    /**
     * get an OAuth20GrantTypeHandler instance
     * 
     * @return
     */
    public OAuth20GrantTypeHandler getHandlerInstance();

}