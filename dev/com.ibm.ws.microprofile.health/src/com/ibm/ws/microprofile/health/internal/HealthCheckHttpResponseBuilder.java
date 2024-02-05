/*******************************************************************************
 * Copyright (c) 2017, 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private int numOfChecks = 0;

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

        if (numOfChecks == 0) {
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
                System.out.println("HERE2!");

                data.add(entry.getKey(), entry.getValue().toString());
                System.out.println("HERE3!" + entry.getValue().toString());

            }
            check.add(HEALTH_CHECK_PAYLOAD_DATA, data.build());
        }

        checks.add(check);
        numOfChecks++;

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
