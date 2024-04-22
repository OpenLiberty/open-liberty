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
package io.openliberty.microprofile.metrics.internal.fat.badHistogramTimer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Metric;

@ApplicationScoped
@Path("/test")
public class MetricsResource {

	@Inject
	MetricRegistry metricRegistry;

	@GET
	@Path("/badHistogramPercentiles")
	public String badHistogramPercentiles() throws InterruptedException {
		// Histogram histogram = metricRegistry
		// .histogram("badHistogramPercentiles");
		return "Histogram registered";
	}
	//

	@Inject
	@Metric(name = "badHistogramPercentiles", absolute = true)
	private Histogram badHistogramPercentiles;

	@Inject
	@Metric(name = "badTimerPercentiles", absolute = true)
	private Timer badTimerPercentiles;

	@Inject
	@Metric(name = "badHistogramBuckets", absolute = true)
	private Histogram badHistogramBuckets;

	@Inject
	@Metric(name = "badTimerBuckets", absolute = true)
	private Timer badTimerBuckets;

}
