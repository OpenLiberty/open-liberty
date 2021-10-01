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
package io.openliberty.grpc.internal.config;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.grpc.internal.GrpcMessages;

public class GrpcServiceConfigValidation {

    private static final TraceComponent tc = Tr.register(GrpcServiceConfigValidation.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

    /**
     * @param value - key name
     * @return true if max inbound message size value is valid
     */
    static boolean validateMaxInboundMessageSize(String value) {
        int size = Integer.parseInt(value);
        if (size < 1) {
            Tr.error(tc, "invalid.inbound.msg.size", size);
            return false;
        }
        return true;
    }
}
