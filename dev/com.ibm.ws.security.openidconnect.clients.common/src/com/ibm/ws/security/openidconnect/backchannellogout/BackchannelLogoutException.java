/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout;

import javax.servlet.http.HttpServletResponse;

public class BackchannelLogoutException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int responseCode;

    public BackchannelLogoutException(int responseCode) {
        super();
        this.responseCode = responseCode;
    }

    public BackchannelLogoutException(String errorMsg) {
        super(errorMsg);
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
