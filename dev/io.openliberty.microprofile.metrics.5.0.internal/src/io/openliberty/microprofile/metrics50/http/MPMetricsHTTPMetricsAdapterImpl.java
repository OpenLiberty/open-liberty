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

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.http.monitor.HttpStatAttributes;
import io.openliberty.http.monitor.metrics.HTTPMetricAdapter;
import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

/**
 *
 */
@Component(service = { HTTPMetricAdapter.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPMetricsHTTPMetricsAdapterImpl implements HTTPMetricAdapter {

    static SharedMetricRegistries sharedMetricRegistries;

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

        Metadata md = new MetadataBuilder().withName("http.server.request.duration")
                .withDescription("Duration of HTTP server requests").build();

        Timer httpTimer = vendorRegistry.timer(md, retrieveTags(httpStatAttributes));
        httpTimer.update(duration);

    }

    private Tag[] retrieveTags(HttpStatAttributes httpStatAttributes) {

        Tag requestMethod = new Tag("http_request_method", httpStatAttributes.getRequestMethod());
        Tag scheme = new Tag("url_scheme", httpStatAttributes.getScheme());

        Integer status = httpStatAttributes.getResponseStatus().orElse(-1);
        Tag responseStatusTag = new Tag("http_response_status_code", status == -1 ? "" : status.toString().trim());

        Tag httpRouteTag = new Tag("http_route", httpStatAttributes.getHttpRoute().orElse(""));

        Tag networkProtoclNameTag = new Tag("network_protocol_name", httpStatAttributes.getNetworkProtocolName());
        Tag networkProtocolVersionTag = new Tag("network_protocol_version",
                httpStatAttributes.getNetworkProtocolVersion());

        Tag serverNameTag = new Tag("server_address", httpStatAttributes.getServerName());
        Tag serverPortTag = new Tag("server_port", String.valueOf(httpStatAttributes.getServerPort()));

        String errorType = httpStatAttributes.getErrorType().orElse("");
        Tag errorTypeTag = new Tag("error_type", errorType);

        Tag[] ret = new Tag[] { requestMethod, scheme, responseStatusTag, httpRouteTag, networkProtoclNameTag,
                networkProtocolVersionTag, serverNameTag, serverPortTag, errorTypeTag };

        return ret;
    }

}
