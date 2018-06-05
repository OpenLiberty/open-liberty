/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.utils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ConfigUtils {
	private static final TraceComponent tc = Tr.register(ConfigUtils.class);
	public final static String CFG_DEFAULT_COOKIENAME = "JWT"; // needs to match
																// metatype.xml
	private final static String validCookieChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$%&'*+-.^_`|~"; // rfc6265

	/**
	 * reset cookieName to default value if it is not valid
	 *
	 * @param cookieName
	 * @param quiet
	 *            don't emit any error messages
	 * @return original name or default if original was invalid
	 */
	public String validateCookieName(String cookieName, boolean quiet) {
		if (cookieName == null || cookieName.length() == 0) {
			if (!quiet) {
				Tr.error(tc, "COOKIE_NAME_CANT_BE_EMPTY");
			}
			return CFG_DEFAULT_COOKIENAME;
		}
		String cookieNameUc = cookieName.toUpperCase();
		boolean valid = true;
		for (int i = 0; i < cookieName.length(); i++) {
			String eval = cookieNameUc.substring(i, i + 1);
			if (!validCookieChars.contains(eval)) {
				if (!quiet) {
					Tr.error(tc, "COOKIE_NAME_INVALID", new Object[] { cookieName, eval });
				}
				valid = false;
			}
		}
		if (!valid) {
			return CFG_DEFAULT_COOKIENAME;
		} else {
			return cookieName;
		}
	}

}
