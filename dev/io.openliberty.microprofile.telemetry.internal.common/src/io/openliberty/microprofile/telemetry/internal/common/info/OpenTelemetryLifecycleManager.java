/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.info;

import com.ibm.ws.runtime.metadata.ApplicationMetaData;

/**
 *
 */
public interface OpenTelemetryLifecycleManager {
    /**
     * @return
     */
    OpenTelemetryInfo getOpenTelemetryInfo();

    /**
     * @param metaData
     * @return
     */
    OpenTelemetryInfo getOpenTelemetryInfo(ApplicationMetaData metaData);

    /**
     * @return
     */
    boolean isRuntimeEnabled();

}
