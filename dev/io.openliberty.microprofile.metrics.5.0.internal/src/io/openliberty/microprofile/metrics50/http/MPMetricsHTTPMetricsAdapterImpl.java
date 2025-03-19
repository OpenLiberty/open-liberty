/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.http;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

import io.openliberty.http.monitor.HttpStatAttributes;
import io.openliberty.http.monitor.metrics.HTTPMetricAdapter;
import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.microprofile.metrics50.helper.Constants;

/**
 *
 */
@Component(service = { HTTPMetricAdapter.class, ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPMetricsHTTPMetricsAdapterImpl implements HTTPMetricAdapter, ApplicationStateListener {

    static SharedMetricRegistries sharedMetricRegistries;

    private static final TraceComponent tc = Tr.register(MPMetricsHTTPMetricsAdapterImpl.class);

    private static final String NO_APP_NAME_IDENTIFIER = "io.openliberty.microprofile.metrics50.internal.http.no.app.name";

    /**
     * Mapping between application name to a map of HTTP stats ID mapped to
     * MicroProfile Metrics' Tags i.e. Map<appName, Map<HttpStatID, Tags>>
     */
    private static Map<String, Map<String, Tag[]>> appNameToTagsMap = new ConcurrentHashMap<String, Map<String, Tag[]>>();

    @Activate
    public void activate() {
    }

    @Reference
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        MPMetricsHTTPMetricsAdapterImpl.sharedMetricRegistries = sharedMetricRegistry;
    }

    @Override
    public void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration) {

        if (sharedMetricRegistries == null) {
            return;
        }

        MetricRegistry vendorRegistry = sharedMetricRegistries.getOrCreate(MetricRegistry.VENDOR_SCOPE);

        Metadata md = new MetadataBuilder().withName(Constants.HTTP_SERVER_REQUEST_DURATION_NAME)
                .withDescription(Constants.HTTP_SERVER_REQUEST_DURATION_DESC).build();

        String appName = getApplicationName();
        appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

        String keyID = httpStatAttributes.getHttpStatID();

        // Key is the HttpStasID generated for each httpStatsAttribute
        Map<String, Tag[]> attributesMap = appNameToTagsMap.computeIfAbsent(appName,
                x -> new ConcurrentHashMap<String, Tag[]>());
        Tag[] tags = attributesMap.computeIfAbsent(keyID, x -> retrieveTags(httpStatAttributes));

        Timer httpTimer = vendorRegistry.timer(md, tags);
        httpTimer.update(duration);

    }

    private Tag[] retrieveTags(HttpStatAttributes httpStatAttributes) {

        Tag requestMethodTag = new Tag(Constants.HTTP_REQUEST_METHOD, httpStatAttributes.getRequestMethod());
        Tag urlSchemeTag = new Tag(Constants.URL_SCHEME, httpStatAttributes.getScheme());

        int status = httpStatAttributes.getResponseStatus();

        Tag responseStatusTag = new Tag(Constants.HTTP_RESPONSE_STATUS_CODE,
                (status == -1 ? "" : Integer.toString(status)));

        String httpRoute = httpStatAttributes.getHttpRoute();
        Tag httpRouteTag = new Tag(Constants.HTTP_ROUTE, (httpRoute == null ? "" : httpRoute));

        Tag networkProtocolVersionTag = new Tag(Constants.NETWORK_PROTOCOL_VERSION,
                httpStatAttributes.getNetworkProtocolVersion());

        Tag serverNameTag = new Tag(Constants.SERVER_ADDRESS, httpStatAttributes.getServerName());
        Tag serverPortTag = new Tag(Constants.SERVER_PORT, String.valueOf(httpStatAttributes.getServerPort()));

        String errorType = httpStatAttributes.getErrorType();

        Tag errorTypeTag = new Tag(Constants.ERROR_TYPE, (errorType == null ? "" : errorType));

        Tag[] ret = new Tag[] { requestMethodTag, urlSchemeTag, responseStatusTag, httpRouteTag,
                networkProtocolVersionTag, serverNameTag, serverPortTag, errorTypeTag };

        return ret;
    }

    private String getApplicationName() {
        ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl
                .getComponentMetaDataAccessor().getComponentMetaData();
        if (metaData != null) {
            J2EEName name = metaData.getJ2EEName();
            if (name != null) {
                return name.getApplication();
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getDeploymentName();
        Map<String, Tag[]> map = appNameToTagsMap.remove(appName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format(
                    "Detected that application %s has stopped. Removed a corresponding Map<String, Attributes> entry? [%b]",
                    appName, (map != null)));
        }
    }

}
