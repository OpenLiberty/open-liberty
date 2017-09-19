/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.State;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONFactory;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.jsonsupport.JSONSettings;
import com.ibm.websphere.jsonsupport.JSONSettings.Include;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HealthCheckHttpResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheckHttpResponseBuilder.class);

    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final String HEALTH_CHECK_PAYLOAD_OUTCOME = "outcome";
    private static final String HEALTH_CHECK_PAYLOAD_CHECKS = "checks";
    private static final String HEALTH_CHECK_PAYLOAD_NAME = "name";
    private static final String HEALTH_CHECK_PAYLOAD_STATE = "state";
    private static final String HEALTH_CHECK_PAYLOAD_DATA = "data";

    private State overallState = State.UP;
    private final ArrayList<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();

    JSON json = null;

    void addResponses(Set<HealthCheckResponse> hcResponseSet) {
        Iterator<HealthCheckResponse> hcResponseIt = hcResponseSet.iterator();
        while (hcResponseIt.hasNext()) {
            HealthCheckResponse hcResponse = hcResponseIt.next();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addResponsek(): hcResponse = " + hcResponse);
            setChecks(hcResponse);
        }
    }

    void setHttpResponse(HttpServletResponse httpResponse) {
        httpResponse.setHeader(HTTP_HEADER_CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON);
        HashMap<String, Object> payload = new HashMap<String, Object>();

        // No health check procedure found
        if (checks.isEmpty()) {
            httpResponse.setStatus(200);
            payload.put(HEALTH_CHECK_PAYLOAD_OUTCOME, State.UP);
        } else { // health check state is UP or DOWN
            payload.put(HEALTH_CHECK_PAYLOAD_OUTCOME, overallState);
            httpResponse.setStatus(overallState == State.UP ? 200 : 503);
        }

        payload.put(HEALTH_CHECK_PAYLOAD_CHECKS, checks.toArray());
        setJSONPayload(payload, httpResponse);
    }

    /**
     * Request processing failed (i.e. error in health check procedure)
     */
    public void handleUndeterminedResponse(HttpServletResponse httpResponse) {
        httpResponse.setStatus(500);
    }

    //--------------------------------

    private void setChecks(HealthCheckResponse response) {

        HashMap<String, Object> check = new HashMap<String, Object>();
        check.put(HEALTH_CHECK_PAYLOAD_NAME, response.getName());

        State checkState = response.getState();
        check.put(HEALTH_CHECK_PAYLOAD_STATE, checkState);
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
            check.put(HEALTH_CHECK_PAYLOAD_DATA, data.get());

        checks.add(check);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setChecks(): checks = " + checks);
    }

    private void setJSONPayload(Map<String, Object> payload, HttpServletResponse httpResponse) {
        try {
            JSON jsonService = getJSON();
            httpResponse.getOutputStream().write(jsonService.asBytes(payload));
        } catch (IOException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected IOException while writing out POJO response", e);
            }
            httpResponse.setStatus(500);
        } catch (JSONMarshallException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected JSONMarshallException while getting the JSON service", e);
            }
            httpResponse.setStatus(500);
        }
    }

    /**
     * Utility that returns a JSON object from a factory
     *
     * @return the JSON object providing POJO-JSON serialization and deserialization
     * @throws JSONMarshallException if there are problems configuring serialization inclusion
     */
    private JSON getJSON() throws JSONMarshallException {
        if (json == null) {
            JSONSettings settings = new JSONSettings(Include.NON_NULL);
            json = JSONFactory.newInstance(settings);
        }
        return json;
    }
}
