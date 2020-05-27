/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.grpc.config;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class GrpcServiceConfigValidation {

	private static final TraceComponent tc = Tr.register(GrpcServiceConfigValidation.class);

	/**
	 * @param value - key name
	 * @return true if max inbound message size value is valid
	 */
	static boolean validateMaxInboundMessageSize(String value) {
		int size = Integer.parseInt(value);
		if (size < 1) {
			Tr.warning(tc, "grpcTarget maxInboundMessageSize is invalid", size);
			return false;
		}
		return true;
	}
}
