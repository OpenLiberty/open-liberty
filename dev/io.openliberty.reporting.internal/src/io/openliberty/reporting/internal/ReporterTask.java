/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.feature.FixManager;

/**
 * If the feature is enabled the FixReportingComponent will run this task.
 */
public class ReporterTask implements Runnable {

    private static final TraceComponent tc = Tr.register(ReporterTask.class);

    private final FeatureProvisioner featureProvisioner;
    private final FixManager fixManager;
    private final ServerInfoMBean serverInfo;
    private final Map<String, Object> props;

    public ReporterTask(FeatureProvisioner featureProvisioner, FixManager fixManager, ServerInfoMBean serverInfo,
                        Map<String, Object> properties) {
        this.featureProvisioner = featureProvisioner;
        this.fixManager = fixManager;
        this.serverInfo = serverInfo;
        this.props = properties;
    }

    /**
     * <p>
     * 1. Calls the DataCollector
     * 2. Retrieves the CVE Data from the cloud service
     * 3. Handles the response
     * </p>
     */
    @Override
    @FFDCIgnore({ DataCollectorException.class, MalformedURLException.class, IOException.class })
    public void run() {
        // Run if disabled flag has not been found
        try {
            DataCollector collector = new DataCollector(featureProvisioner, fixManager, serverInfo);
            Map<String, String> data = collector.getData();
            String urlLink = (String) props.get("urlLink");
            JSONObject response = new CVEServiceClient().retrieveCVEData(data, urlLink);
            CVEResponseHandler.handleResponse(data.get("productEdition"), response);
        } catch (MalformedURLException e) {
            Tr.warning(tc, "CWWKF1704.incorrect.url", e.getMessage());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caused by: " + e);
            }
        } catch (IOException e) {
            Tr.warning(tc, "CWWKF1705.failed.response");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                String causes = buildExceptionMessage(e);
                Tr.debug(tc, "Failed due to: " + causes);
            }

        } catch (DataCollectorException e) {
            Tr.warning(tc, "CWWKF1705.issue.parsing");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                String causes = buildExceptionMessage(e);
                Tr.debug(tc, "Failed due to: " + causes);
            }
        }
    }

    /**
     * <p>
     * Creates a string with the type and message of a Throwable and its causes.
     * </p>
     *
     * <pre>
     * 	Example:
     * 			com.example.MyException: My Exception Message: com.example.MyException: The cause of my exception
     * </pre>
     *
     * @param e The Throwable
     * @return A string with the type and message of a Throwable and its causes.
     */
    public static String buildExceptionMessage(Throwable e) {

        Throwable cause = null;
        Throwable e2 = e;

        StringBuilder causes = new StringBuilder(e2.toString());

        while (null != (cause = e2.getCause())) {
            causes.append(": ");
            causes.append(cause.toString());
            e2 = cause;
        }

        return causes.toString();
    }

}
