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
package io.openliberty.microprofile.openapi20.utils;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class LoggingUtils {
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
    
    private LoggingUtils() {
        // This class is not meant to be instantiated.
    }
}
