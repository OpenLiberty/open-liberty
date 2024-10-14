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
import io.openliberty.microprofile.metrics50.helper.Constants;

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

        Metadata md = new MetadataBuilder().withName(Constants.HTTP_SERVER_REQUEST_DURATION_NAME)
                .withDescription(Constants.HTTP_SERVER_REQUEST_DURATION_DESC).build();

        Timer httpTimer = vendorRegistry.timer(md, retrieveTags(httpStatAttributes));
        httpTimer.update(duration);

    }

    private Tag[] retrieveTags(HttpStatAttributes httpStatAttributes) {

        Tag requestMethodTag = new Tag(Constants.HTTP_REQUEST_METHOD, httpStatAttributes.getRequestMethod());
        Tag urlSchemeTag = new Tag(Constants.URL_SCHEME, httpStatAttributes.getScheme());

        Integer status = httpStatAttributes.getResponseStatus().orElse(-1);
        Tag responseStatusTag = new Tag(Constants.HTTP_RESPONSE_STATUS_CODE,
                status == -1 ? "" : status.toString().trim());

        Tag httpRouteTag = new Tag(Constants.HTTP_ROUTE, httpStatAttributes.getHttpRoute().orElse(""));

        Tag networkProtocolVersionTag = new Tag(Constants.NETWORK_PROTOCOL_VERSION,
                httpStatAttributes.getNetworkProtocolVersion());

        Tag serverNameTag = new Tag(Constants.SERVER_ADDRESS, httpStatAttributes.getServerName());
        Tag serverPortTag = new Tag(Constants.SERVER_PORT, String.valueOf(httpStatAttributes.getServerPort()));

        String errorType = httpStatAttributes.getErrorType().orElse("");
        Tag errorTypeTag = new Tag(Constants.ERROR_TYPE, errorType);

        Tag[] ret = new Tag[] { requestMethodTag, urlSchemeTag, responseStatusTag, httpRouteTag,
                networkProtocolVersionTag, serverNameTag, serverPortTag, errorTypeTag };

        return ret;
    }

}
