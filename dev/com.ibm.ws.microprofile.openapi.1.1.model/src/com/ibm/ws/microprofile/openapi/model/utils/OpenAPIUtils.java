/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.model.utils;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class OpenAPIUtils {
    private static final TraceComponent tc = Tr.register(OpenAPIUtils.class);

    @Trivial
    public static boolean isDebugEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
    }

    @Trivial
    public static boolean isEventEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled();
    }

    @Trivial
    public static boolean isDumpEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isDumpEnabled();
    }

    /**
     * Print map to string
     *
     * @param map - the map to be printed to String
     */
    public static String mapToString(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (String key : map.keySet()) {
            if (map.get(key) != null) {
                sb.append(key + ": " + map.get(key) + "\n ");
            } else {
                continue;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
