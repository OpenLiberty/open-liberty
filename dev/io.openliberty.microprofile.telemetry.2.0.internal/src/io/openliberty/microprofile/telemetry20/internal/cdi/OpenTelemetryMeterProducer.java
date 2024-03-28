/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package io.openliberty.microprofile.telemetry20.internal.cdi;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.opentelemetry.api.metrics.Meter;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;

@ApplicationScoped
public class OpenTelemetryMeterProducer {

    private static final TraceComponent tc = Tr.register(OpenTelemetryMeterProducer.class);

    @Produces
    public Meter getMeter() {
        //I'm using this rather than an injected paramater for performence.
        return OpenTelemetryAccessor.getOpenTelemetryInfo().getOpenTelemetry().getMeter(OpenTelemetryConstants.INSTRUMENTATION_NAME);
    }
}
