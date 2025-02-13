/*******************************************************************************
 * Copyright (c) 2020, 2025 Contributors to the Eclipse Foundation
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
package io.openliberty.microprofile.health30.internal;

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
import org.eclipse.microprofile.health.HealthCheckResponse.Status;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;

public class HealthCheck30HttpResponseBuilder {

    private static final TraceComponent tc = Tr.register(HealthCheck30HttpResponseBuilder.class);
    private static final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);

    protected Status overallStatus = Status.UP;
    protected final JsonArrayBuilder checks = jsonBuilderFactory.createArrayBuilder();

    public HealthCheck30HttpResponseBuilder() {
    }

    public void addResponses(Set<HealthCheckResponse> hcResponseSet) {
        Iterator<HealthCheckResponse> hcResponseIt = hcResponseSet.iterator();
        while (hcResponseIt.hasNext()) {
            HealthCheckResponse hcResponse = hcResponseIt.next();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addResponse(): hcResponse = " + hcResponse);
            setChecks(hcResponse);
        }
    }

    public void setHttpResponse(HttpServletResponse httpResponse) {
        httpResponse.setHeader(HealthCheckConstants.HTTP_HEADER_CONTENT_TYPE, HealthCheckConstants.MEDIA_TYPE_APPLICATION_JSON);

        JsonObjectBuilder payload = jsonBuilderFactory.createObjectBuilder();

        // Set the HTTP Response code
        httpResponse.setStatus(overallStatus == Status.UP ? 200 : 503);

        // Populate the payload with the overall status and checks array
        payload.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, overallStatus.name());
        payload.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_CHECKS, checks);

        // Convert it into a JSON payload
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
        check.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_NAME, response.getName());

        Status checkStatus = response.getStatus();
        check.add(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, checkStatus == null ? null : checkStatus.name());

        if (checkStatus != null) {
            if (checkStatus.equals(Status.DOWN))
                overallStatus = Status.DOWN;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setChecks(): checkStatus is null");
            overallStatus = Status.DOWN; // treat as fail case
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
     * Sets the overall status for the health check
     *
     * @param status
     */
    public void setOverallStatus(Status status) {
        this.overallStatus = status;
    }

}