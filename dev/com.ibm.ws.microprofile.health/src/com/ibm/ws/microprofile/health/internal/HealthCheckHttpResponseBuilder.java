/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.health.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.State;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HealthCheckHttpResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheckHttpResponseBuilder.class);
    protected static final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);

    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final String HEALTH_CHECK_PAYLOAD_OUTCOME = "outcome";
    private static final String HEALTH_CHECK_PAYLOAD_CHECKS = "checks";
    private static final String HEALTH_CHECK_PAYLOAD_NAME = "name";
    private static final String HEALTH_CHECK_PAYLOAD_STATE = "state";
    private static final String HEALTH_CHECK_PAYLOAD_DATA = "data";

    protected State overallState = State.UP;
    protected final JsonArrayBuilder checks = jsonBuilderFactory.createArrayBuilder();

    private boolean checksExist = false;

    public HealthCheckHttpResponseBuilder() {
    }

    public void addResponses(Set<HealthCheckResponse> hcResponseSet) {
        Iterator<HealthCheckResponse> hcResponseIt = hcResponseSet.iterator();
        while (hcResponseIt.hasNext()) {
            HealthCheckResponse hcResponse = hcResponseIt.next();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addResponsek(): hcResponse = " + hcResponse);
            setChecks(hcResponse);
        }
    }

    public void setHttpResponse(HttpServletResponse httpResponse) {
        httpResponse.setHeader(HTTP_HEADER_CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);
        JsonObjectBuilder payload = jsonBuilderFactory.createObjectBuilder();

        // No health check procedure found

        if (!checksExist) {
            httpResponse.setStatus(200);
            payload.add(HEALTH_CHECK_PAYLOAD_OUTCOME, State.UP.name());
        } else { // health check state is UP or DOWN
            payload.add(HEALTH_CHECK_PAYLOAD_OUTCOME, overallState.name());
            httpResponse.setStatus(overallState == State.UP ? 200 : 503);
        }

        payload.add(HEALTH_CHECK_PAYLOAD_CHECKS, checks);
        setJSONPayload(payload.build(), httpResponse);
    }

    /**
     * Request processing failed (i.e. error in health check procedure)
     */
    public void handleUndeterminedResponse(HttpServletResponse httpResponse) {
        httpResponse.setStatus(500);
    }

    //--------------------------------

    protected void setChecks(HealthCheckResponse response) {

        JsonObjectBuilder check = jsonBuilderFactory.createObjectBuilder();
        check.add(HEALTH_CHECK_PAYLOAD_NAME, response.getName());

        State checkState = response.getState();
        check.add(HEALTH_CHECK_PAYLOAD_STATE, checkState == null ? null : checkState.name());
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
            check.add(HEALTH_CHECK_PAYLOAD_DATA, data.build());
        }

        checks.add(check);
        checksExist = true;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "setChecks(): checks = " + checks);
    }

    protected void setJSONPayload(JsonObject payload, HttpServletResponse httpResponse) {
        try {
            httpResponse.getOutputStream().write(payload.toString().getBytes());
        } catch (IOException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected IOException while writing out POJO response", e);
            }
            httpResponse.setStatus(500);
        }
    }

    /**
     * Sets the overall state for the health check
     *
     * @param state
     */
    public void setOverallState(State state) {
        this.overallState = state;
    }
}
