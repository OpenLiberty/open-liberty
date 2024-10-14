/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

/**
 *
 * <h1> Open Liberty MicroProfile Telemetry Interface</h1>
 * <br>
 * {@link OpenTelemetryAccessor} provides one method: {@link OpenTelemetryAccessor.getOpenTelemetryInfo} which returns an
 * {@link OpenTelemetryInfo} containing the OpenTelemetry object associated with the currently running application.
 *
 * That would be the runtime instance of OpenTelemetry if one is enabled. If there is not one enabled, it would be chosen
 * based on the application metadata stored in the thread context. If there is no metadata on the thread, or if the application
 * has shut down, it would return an OpenTelemetryInfo containing a no-op OpenTelemetry object.
 *
 * OpenTelemetryInfo provides two methods;
 * <ul>
 * <li>{@link OpenTelemetryInfo.getOpenTelemetry} which returns the contained OpenTelemetry object.</li>
 * <li>{@link OpenTelemetryInfo.isEnabled} which returns true if the contained OpenTelemetry object was enabled at the time this OpenTelemetryInfo was created
 * and false if it was a no-op </li>
 * </ul>
 *
 * The suggested use is to use isEnabled() when initalizing components to avoid enabling code that depends on OpenTelemetry
 * if it is not enabled for a performance gain.
 *
 * <pre>{@code
 * OpenTelemetryInfo openTelemetryInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
 * if (openTelemetryInfo.isEnabled()) {
 *     createOpenTelemetryServletFilter(openTelemetryInfo.getOpenTelemetry());
 * }
 * }</pre>
 */

@TraceOptions(traceGroup = "TELEMETRY", messageBundle = "io.openliberty.microprofile.telemetry.internal.common.resources.MPTelemetry")
package io.openliberty.microprofile.telemetry.spi;

import com.ibm.websphere.ras.annotation.TraceOptions;
