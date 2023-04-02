/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import io.opentelemetry.context.Scope;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;

/**
 * Closes the Scope at the end of the request.
 * <p>
 * Context is set for a thread, so it's important that you start and end a Scope on the same thread.
 * <p>
 * When using async resource methods, the two filter methods of TelemetryContainerFilter may be called on different threads.
 * Using a ServletRequestListener ensures that we close the scope on the original request thread.
 */
public class TelemetryServletRequestListener implements ServletRequestListener {

    /** {@inheritDoc} */
    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        // End the span scope (if present)
        Scope scope = (Scope) sre.getServletRequest().getAttribute(TelemetryContainerFilter.SPAN_SCOPE);
        if (scope != null) {
            scope.close();
            sre.getServletRequest().removeAttribute(TelemetryContainerFilter.SPAN_SCOPE);
        }
    }

}
