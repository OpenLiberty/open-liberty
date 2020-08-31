/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt;

import com.ibm.ws.security.jwt.config.JwtConsumerConfig;

public interface MicroProfileJwtConfig extends JwtConsumerConfig {

    /**
     *
     * @return Id of the mpJwt configuration
     */
    public String getUniqueId();

    public String getUserNameAttribute();

    public String getGroupNameAttribute();

    public String getAuthorizationHeaderScheme();

    public boolean ignoreApplicationAuthMethod();

    public boolean getMapToUserRegistry();

    public String getAuthFilterRef();

    public String getTokenHeader();

    public String getCookieName();

}
