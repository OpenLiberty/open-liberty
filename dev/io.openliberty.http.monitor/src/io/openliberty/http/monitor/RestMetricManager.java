/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.time.Duration;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

@Component(configurationPolicy = IGNORE)
public class RestMetricManager {

	
	private static final TraceComponent tc = Tr.register(HttpStatsMonitor.class);
	
    static SharedMetricRegistries SHARED_METRIC_REGISTIRES = null;
	
	@Reference  // static and unary - so it MUST have it to activate
	public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistries) {
		System.out.println("SET SharedMetricREgistreis REST MetricManagr");
		if (SHARED_METRIC_REGISTIRES == null) {
			SHARED_METRIC_REGISTIRES = sharedMetricRegistries;
		} else {
			Tr.debug(tc, "Multiple Metric Registries' service-component active");
			System.err.println("Multiple Metric Registries' service-component active");
		}

	}
	
	public void unsetSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistries) {
		System.out.println("UNNNNSET SharedMetricREgistreis REST MetricManagr");
		SHARED_METRIC_REGISTIRES = null;
	}
	
	
	public static void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration) {
		
		if(SHARED_METRIC_REGISTIRES == null) {
			return;
		}
		
		MetricRegistry vendorRegistry = SHARED_METRIC_REGISTIRES.getOrCreate(MetricRegistry.VENDOR_SCOPE);
		
		Metadata md = new MetadataBuilder().withName("http.server.request.duration").build();
		
		Timer httpTimer = vendorRegistry.timer(md,retrieveTags(httpStatAttributes));
		httpTimer.update(duration);
		
	}
	
	private static Tag[] retrieveTags(HttpStatAttributes httpStatAttributes) {

		
		Tag requestMethod = new Tag("request_method", httpStatAttributes.getRequestMethod() );
		Tag scheme = new Tag("http_scheme", httpStatAttributes.getScheme());

		
		Integer status = httpStatAttributes.getResponseStatus().orElse(-1);
		Tag responseStatusTag = new Tag("response_status", status == -1 ? "" : status.toString().trim());
		
		Tag httpRouteTag = new Tag("http_route", httpStatAttributes.getHttpRoute().orElse(""));
		
		Tag networkProtoclNameTag = new Tag("network_name",httpStatAttributes.getNetworkProtocolName());
		Tag networkProtocolVersionTag = new Tag("network_version",httpStatAttributes.getNetworkProtocolVersion());
		
		
		Tag serverNameTag = new Tag("server_name",httpStatAttributes.getServerName());
		Tag serverPortTag = new Tag("server_port",String.valueOf(httpStatAttributes.getServerPort()));
		
		
		String errorType = httpStatAttributes.getErrorType().orElse("");		
		Tag errorTypeTag = new Tag("error_type", errorType);
		
		Tag[] ret = new Tag[] {requestMethod, scheme, responseStatusTag, httpRouteTag, networkProtoclNameTag, networkProtocolVersionTag, serverNameTag, serverPortTag, errorTypeTag};
		
		return ret;
	}
	
}
