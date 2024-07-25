/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.metrics;

import java.time.Duration;

import io.openliberty.http.monitor.HttpStatAttributes;

/**
 * Intended to be a service-component.
 * Implemented by subsequent Metric run-times in their respective bundles.
 */
public interface HTTPMetricAdapter {
	
	/**
	 * Given the HttpStatAttributes, update the HTTP metric of the respective Metrics runtime
	 * 
	 * @param httpStatAttributes
	 * @param duration
	 */
	public void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration);
}
