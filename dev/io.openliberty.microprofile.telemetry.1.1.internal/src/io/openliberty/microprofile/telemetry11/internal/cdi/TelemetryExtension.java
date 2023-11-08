/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;

public class TelemetryExtension implements Extension {

    public void removeInterceptor(@Observes ProcessAnnotatedType<WithSpanInterceptor> pat) {
        if (AgentDetection.isAgentActive()) {
            pat.veto();
        }
    }
}
