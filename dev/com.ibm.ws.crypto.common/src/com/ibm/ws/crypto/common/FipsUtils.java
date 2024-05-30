/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.common;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class FipsUtils {

	public static boolean isFIPSEnabled() {
		String fipsEnabled = AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				return System.getProperty("com.ibm.jsse2.usefipsprovider");
			}
		});
		return Boolean.parseBoolean(fipsEnabled);
	}
}
