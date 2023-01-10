/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.exceptions;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class UserInfoEndpointNotHttpsException extends UserInfoResponseException {

    public static final TraceComponent tc = Tr.register(UserInfoEndpointNotHttpsException.class);

    private static final long serialVersionUID = 1L;

    public UserInfoEndpointNotHttpsException(String userInfoEndpoint, String clientId) {
        super(userInfoEndpoint, Tr.formatMessage(tc, "URL_NOT_HTTPS", userInfoEndpoint, clientId));
    }

}
