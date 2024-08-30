/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.spi;

import java.util.Optional;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.microprofile.telemetry.internal.common.info.ErrorOpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryLifecycleManager;

public class OpenTelemetryAccessor {
    private static final TraceComponent tc = Tr.register(OpenTelemetryAccessor.class);

    private static final ServiceCaller<OpenTelemetryLifecycleManager> openTelemetryLifecycleManagerService = new ServiceCaller<OpenTelemetryLifecycleManager>(OpenTelemetryAccessor.class, OpenTelemetryLifecycleManager.class);
    private static boolean issuedBetaMessage = false;

    //See https://github.com/open-telemetry/opentelemetry-java-docs/blob/main/otlp/src/main/java/io/opentelemetry/example/otlp/ExampleConfiguration.java
    /**
     * Gets or creates the instance of OpenTelemetry associated with this application and returns it wrapped inside an OpenTelemetryInfo.
     * <p>
     * If OpenTelemetry has a runtime instance this will be returned for all applications. If it does not, it will use the application metadata
     * from the current thread to find the instance of OpenTelemetry associated with this application. If there is no metadata on the thread,
     * or if the application has shut down, it will return an OpenTelemetryInfo containing a no-op OpenTelemetry object.
     *
     * @return An instance of OpenTelemetryInfo containing the instance of OpenTelemetry associated with this application.
     */
    @Deprecated
    public static OpenTelemetryInfo getOpenTelemetryInfo() {

        if (ProductInfo.getBetaEdition()) {
            Optional<OpenTelemetryInfo> openTelemetryInfo = openTelemetryLifecycleManagerService.call((factory) -> {
                return factory.getOpenTelemetryInfo();
            });

            return openTelemetryInfo.orElseGet(ErrorOpenTelemetryInfo::getInstance);
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.warning(tc, "BETA: A beta method has been invoked for the class OpenTelemetryAccessor for the first time.");
                issuedBetaMessage = true;
            }
            throw new UnsupportedOperationException("This feature is still in beta, add -Dcom.ibm.ws.beta.edition=true to jvm.options if you wish to use beta code");
        }
    }
}
