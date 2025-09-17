/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.telemetry.user.wab;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.ext.Provider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.spi.OpenTelemetryAccessor;
import io.openliberty.microprofile.telemetry.spi.OpenTelemetryInfo;

@Provider
public class WabFeatureServletFilter implements Filter {

    private static final TraceComponent tc = Tr.register(WabFeatureServletFilter.class);

    @Override
    public void destroy() {
        //do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("UserFeatureTest: Entering wab feature  doFilter");

        //Test that OpenTelemetry doesn't crash if we call the accessor inside a WAB
        //isEnabled will be true in runtimeMode and false in appMode. The test servlets will verify this
        OpenTelemetryInfo openTelemetryInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
        if (openTelemetryInfo.isEnabled()) {
            request.setAttribute("telemetryEnabled", "true");
        } else {
            request.setAttribute("telemetryEnabled", "false");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //do nothing
    }

}
