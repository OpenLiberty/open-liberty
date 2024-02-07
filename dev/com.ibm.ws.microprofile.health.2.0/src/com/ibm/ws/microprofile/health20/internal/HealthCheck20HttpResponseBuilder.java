/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.health20.internal;

import java.util.Map;
import java.util.Optional;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.State;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.health.internal.HealthCheckHttpResponseBuilder;

public class HealthCheck20HttpResponseBuilder extends HealthCheckHttpResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheck20HttpResponseBuilder.class);

    public HealthCheck20HttpResponseBuilder() {
    }

    @Override
    public void setHttpResponse(HttpServletResponse httpResponse) {
        httpResponse.setHeader(HealthCheckConstants.HTTP_HEADER_CONTENT_TYPE, HealthCheckConstants.MEDIA_TYPE_APPLICATION_JSON);
        JsonObjectBuilder payload = jsonBuilderFactory.createObjectBuilder();

        // Set the HTTP Response code
        httpResponse.setStatus(overallState == State.UP ? 200 : 503);

        // Populate the payload with the overall status and checks array
        payload.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, overallState.name());
        payload.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_CHECKS, checks);

        // Convert it into a JSON payload
        setJSONPayload(payload.build(), httpResponse);
    }

    @Override
    protected void setChecks(HealthCheckResponse response) {

        JsonObjectBuilder check = jsonBuilderFactory.createObjectBuilder();
        check.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_NAME, response.getName());

        State checkState = response.getState();
        check.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, checkState == null ? null : checkState.name());
        if (checkState != null) {
            if (checkState.equals(State.DOWN))
                overallState = State.DOWN;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setChecks(): checkState is null");
            overallState = State.DOWN; // treat as fail case
        }

        Optional<Map<String, Object>> dataMap = response.getData();
        if ((dataMap != null) && dataMap.isPresent()) {
            JsonObjectBuilder data = jsonBuilderFactory.createObjectBuilder();
            for (Map.Entry<String, Object> entry : dataMap.get().entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    data.add(entry.getKey(), JsonValue.NULL);
                } else if (value instanceof String) {
                    data.add(entry.getKey(), (String) value);
                } else if (value instanceof Boolean) {
                    data.add(entry.getKey(), (Boolean) value);
                } else {
                    data.add(entry.getKey(), (Long) value);
                }
            }
            check.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_DATA, data.build());
        }

        checks.add(check);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setChecks(): checks = " + checks);
    }
}
