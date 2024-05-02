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

import com.ibm.ws.kernel.productinfo.ProductInfo;


public class FipsUtils {

	public static boolean isFIPSEnabled() {
		// TODO remove beta mode check
        return isRunningBetaMode() && isRunningFIPS140Dash3Mode();
	}

    static boolean isRunningFIPS140Dash3Mode() {
		String fipsEnabled = AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				return System.getProperty("com.ibm.fips.mode");
			}
		});
		return "140-3".equals(fipsEnabled);
	}

	private static boolean isRunningBetaMode() {
		return ProductInfo.getBetaEdition();
	}
}
