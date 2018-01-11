/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt;

public interface MicroProfileJwtConfig {

    /**
     *
     * @return Id of the mpJwt configuration
     */
    public String getUniqueId();

    public String getUserNameAttribute();

    public String getGroupNameAttribute();

    public boolean ignoreApplicationAuthMethod();

    public boolean getMapToUserRegistry();

}
