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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout;

import javax.servlet.http.HttpServletResponse;

public class BackchannelLogoutException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int responseCode;

    public BackchannelLogoutException(String errorMsg) {
        this(errorMsg, HttpServletResponse.SC_BAD_REQUEST);
    }

    public BackchannelLogoutException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
        this.responseCode = HttpServletResponse.SC_BAD_REQUEST;
    }

    public BackchannelLogoutException(String errorMsg, int responseCode) {
        super(errorMsg);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

}
