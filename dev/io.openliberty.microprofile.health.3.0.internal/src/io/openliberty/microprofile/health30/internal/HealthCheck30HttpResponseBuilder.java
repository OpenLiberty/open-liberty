/*******************************************************************************
 * Copyright (c) 2020, 2024 Contributors to the Eclipse Foundation
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
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
    protected final ArrayList<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();

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
        HashMap<String, Object> payload = new HashMap<String, Object>();

        // Set the HTTP Response code
        httpResponse.setStatus(overallStatus == Status.UP ? 200 : 503);

        // Populate the payload with the overall status and checks array
        payload.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, overallStatus.name());
        payload.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_CHECKS, checks);

        // Convert it into a JSON payload
        setJSONPayload(payload, httpResponse);
    }

    /**
     * Request processing failed (i.e. error in health check procedure)
     */
    public void handleUndeterminedResponse(HttpServletResponse httpResponse) {
        httpResponse.setStatus(500);
    }

    //--------------------------------

    protected void setChecks(HealthCheckResponse response) {

        HashMap<String, Object> check = new HashMap<String, Object>();
        check.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_NAME, response.getName());

        Status checkStatus = response.getStatus();
        check.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_STATUS, checkStatus == null ? checkStatus : checkStatus.name());

        if (checkStatus != null) {
            if (checkStatus.equals(Status.DOWN))
                overallStatus = Status.DOWN;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setChecks(): checkStatus is null");
            overallStatus = Status.DOWN; // treat as fail case
        }

        Optional<Map<String, Object>> data = response.getData();
        if ((data != null) && data.isPresent()) {
            check.put(HealthCheckConstants.HEALTH_CHECK_PAYLOAD_DATA, data.get());
        }

        checks.add(check);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setChecks(): checks = " + checks);
    }

    protected void setJSONPayload(Map<String, Object> payload, HttpServletResponse httpResponse) {
        try {
            httpResponse.getOutputStream().write(jsonBuilderFactory.createObjectBuilder(payload).build().toString().getBytes());
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