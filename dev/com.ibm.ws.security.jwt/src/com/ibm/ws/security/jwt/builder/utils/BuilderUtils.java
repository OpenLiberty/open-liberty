/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.ws.security.jwt.builder.utils;

import java.util.Map;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.security.jwt.internal.ClaimsImpl;
import com.ibm.ws.security.jwt.utils.JwtUtils;

public class BuilderUtils {

	public BuilderUtils() {

	}

	public Claims parseJwtForClaims(String jwtPayloadString) throws Exception {
		Claims claims = null;
		if (jwtPayloadString != null) {
			Map map = JwtUtils.claimsFromJsonObject(jwtPayloadString);
			claims = new ClaimsImpl();
			claims.putAll(map);
		}
		return claims;
	}

}
