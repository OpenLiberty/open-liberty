/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.microprofile.metrics.internal.fat.timer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@ApplicationScoped
@Path("/test")
public class MetricsResource {

	@GET
	@Path("/getTimers")
	public String badHistogramPercentiles() throws InterruptedException {
		return "Histogram registered";
	}

	@Inject
	@Metric(name = "injectedTimerCustomPercentiles")
	private Timer injectedTimerCustomPercentiles;

	@Inject
	@Metric(name = "injectedTimerNoPercentiles")
	private Timer injectedTimerNoPercentiles;

	@Inject
	@Metric(name = "injectedTimerCustomBucketsDefaultPercentiles")
	private Timer injectedTimerCustomBucketsDefaultPercentiles;

	@Inject
	@Metric(name = "injectedTimerCustomBucketsCustomPercentiles")
	private Timer injectedTimerCustomBucketsCustomPercentiles;

	@Inject
	@Metric(name = "injectedTimerCustomBucketsNoPercentiles")
	private Timer injectedTimerCustomBucketsNoPercentiles;

	@Inject
	@Metric(name = "injected.precedence.timer", absolute = true)
	private Timer injectedPrecedenceTimer;

	@Inject
	@Metric(name = "injected.precedence.override.timer", absolute = true)
	private Timer injectedPrecedenceOverrideTimer;

}
