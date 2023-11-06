/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.telemetry11.internal.rest.javax;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

import io.openliberty.microprofile.telemetry11.internal.rest.TelemetryClientFilter;
import io.openliberty.microprofile.telemetry11.internal.rest.TelemetryContainerFilter;

@Component(configurationPolicy = IGNORE)
public class TelemetryJaxRsProviderRegister implements JaxRsProviderRegister {

    private static final TraceComponent tc = Tr.register(TelemetryJaxRsProviderRegister.class);

    @Override
    @Trivial
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        try {
            if (clientSide) {
                TelemetryClientFilter currentFilter = new TelemetryClientFilter();
                if (currentFilter != null && currentFilter.isEnabled()) {
                    providers.add(currentFilter);
                }
            } else {
                TelemetryContainerFilter currentFilter = new TelemetryContainerFilter();
                if (currentFilter != null && currentFilter.isEnabled()) {
                    providers.add(currentFilter);
                }
            }
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }
}
