/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.State;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.health.internal.HealthCheckHttpResponseBuilder;

public class HealthCheck20HttpResponseBuilder extends HealthCheckHttpResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheck20HttpResponseBuilder.class);

    @Override
    public void setHttpResponse(HttpServletResponse httpResponse) {
        httpResponse.setHeader(HealthCheckConstants.HTTP_HEADER_CONTENT_TYPE, HealthCheckConstants.MEDIA_TYPE_APPLICATION_JSON);
        HashMap<String, Object> payload = new HashMap<String, Object>();

        // Set the HTTP Response code
        httpResponse.setStatus(overallState == State.UP ? 200 : 503);

        // Populate the payload with the overall status and checks array
        payload.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, overallState);
        payload.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_CHECKS, checks.toArray());

        // Convert it into a JSON payload
        setJSONPayload(payload, httpResponse);
    }

    @Override
    protected void setChecks(HealthCheckResponse response) {

        HashMap<String, Object> check = new HashMap<String, Object>();
        check.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_NAME, response.getName());

        State checkState = response.getState();
        check.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, checkState);
        if (checkState != null) {
            if (checkState.equals(State.DOWN))
                overallState = State.DOWN;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setChecks(): checkState is null");
            overallState = State.DOWN; // treat as fail case
        }

        Optional<Map<String, Object>> data = response.getData();
        if ((data != null) && data.isPresent())
            check.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_DATA, data.get());

        checks.add(check);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setChecks(): checks = " + checks);
    }
}
