/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.exceptions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class UserInfoEndpointNotHttpsException extends Exception {

    public static final TraceComponent tc = Tr.register(UserInfoEndpointNotHttpsException.class);

    private static final long serialVersionUID = 1L;

    private final String userInfoEndpoint;

    public UserInfoEndpointNotHttpsException(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

}
