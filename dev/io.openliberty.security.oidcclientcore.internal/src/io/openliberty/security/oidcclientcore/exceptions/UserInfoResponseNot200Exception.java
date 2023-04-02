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

public class UserInfoResponseNot200Exception extends UserInfoResponseException {

    public static final TraceComponent tc = Tr.register(UserInfoResponseNot200Exception.class);

    private static final long serialVersionUID = 1L;

    private final String statusCode;
    private final String responseStr;

    public UserInfoResponseNot200Exception(String userInfoEndpoint, String statusCode, String responseStr) {
        super(userInfoEndpoint, Tr.formatMessage(tc, "USERINFO_RESPONSE_NOT_200", userInfoEndpoint, statusCode, responseStr));
        this.statusCode = statusCode;
        this.responseStr = responseStr;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getResponseStr() {
        return responseStr;
    }

}
