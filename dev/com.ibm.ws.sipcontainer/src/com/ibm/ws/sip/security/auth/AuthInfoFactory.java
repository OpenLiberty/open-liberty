/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.security.auth;

import javax.servlet.sip.AuthInfo;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class AuthInfoFactory {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(AuthInfoFactory.class);

    /**
	 * Create a new authinfo object, for authenticating a SIP request.
	 */
	public static AuthInfo createAuthInfo() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "createAuthInfo");
		}

		AuthInfo authInfo = new AuthInfoImpl();

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "createAuthInfo");
		}
		return authInfo;
	}
}
