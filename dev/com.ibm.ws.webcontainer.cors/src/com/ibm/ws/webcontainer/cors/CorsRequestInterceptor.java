/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.cors;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.cors.internal.TraceConstants;
import com.ibm.ws.webcontainer.osgi.interceptor.RequestInterceptor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = { RequestInterceptor.class },
                name = "com.ibm.ws.webcontainer.cors.request.interceptor",
                configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = { "service.vendor=IBM",
                            RequestInterceptor.INTERCEPT_POINTS_PROPERTY + "=" + RequestInterceptor.INTERCEPT_POINT_AFTER_FILTERS,
                            Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE })
public class CorsRequestInterceptor implements RequestInterceptor {
    private static final TraceComponent tc = Tr.register(CorsRequestInterceptor.class);

    private final String KEY_CORS_HELPER = "corsHelper";
    private final AtomicServiceReference<CorsHelper> corsHelperRef = new AtomicServiceReference<CorsHelper>(KEY_CORS_HELPER);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        corsHelperRef.activate(context);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating CorsRequestInterceptor", properties);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        corsHelperRef.deactivate(context);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating CorsRequestInterceptor, reason=" + reason);
        }
    }

    @Reference(service = CorsHelper.class, name = KEY_CORS_HELPER)
    protected void setCorsHelper(ServiceReference<CorsHelper> corsHelper) {
        corsHelperRef.setReference(corsHelper);
    }

    protected void unsetCorsHelper(ServiceReference<CorsHelper> corsHelper) {
        corsHelperRef.unsetReference(corsHelper);
    }

    /** {@inheritDoc} */
    @Override
    public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) {
        boolean handlingRequestCompleted = false;

        CorsHelper corsHelper = corsHelperRef.getService();
        if (corsHelper == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "OSGi service 'CorsHelper' is not available.");
            }

            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                   TraceNLS.getFormattedMessage(this.getClass(),
                                                                TraceConstants.TRACE_BUNDLE_CORE,
                                                                "OSGI_SERVICE_ERROR",
                                                                new Object[] { "CorsHelper" },
                                                                "CWWKO1001E: OSGi service {0} is not available."));
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Exception occurred while trying to send error 500: \n response="
                                       + response + " : \n exception=" + e);
                }
            }

            // Return 'true' so the request shouldn't continue being processed by other handlers.
            return true;
        }

        if (corsHelper.isCorsSupportEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "CORS is supported. Handling the request.");
            }

            handlingRequestCompleted = corsHelper.handleCorsRequest(request, response);
        }

        return handlingRequestCompleted;
    }
}
