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

public class UserInfoResponseException extends Exception {

    public static final TraceComponent tc = Tr.register(UserInfoResponseException.class);

    private static final long serialVersionUID = 1L;

    protected final String userInfoEndpoint;
    protected final String errorMsg;

    public UserInfoResponseException(String userInfoEndpoint, String errorMsg) {
        this.userInfoEndpoint = userInfoEndpoint;
        this.errorMsg = errorMsg;
    }

    public UserInfoResponseException(String userInfoEndpoint, Exception cause) {
        super(cause);
        this.userInfoEndpoint = userInfoEndpoint;
        this.errorMsg = cause.toString();
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "USERINFO_RESPONSE_ERROR", userInfoEndpoint, errorMsg);
    }

}
