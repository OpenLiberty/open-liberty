/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor.metrics.service;

import java.time.Duration;

/**
 * This allows for the JAX-RS/Restful-WS filter to manage the creation and
 * updates of REST metrics in "down-stream" metric runtimes. This is so that all
 * timings would be the same. The implementation class will register this
 * interface as an OSGI service which would be consumed as a service reference
 * in this bundle.
 * 
 */
public interface RestMetricsCallback {

	/**
	 * Create the REST metric
	 * 
	 * @param classMethodParamSignature The class and method signature:
	 *                                  class/method(paramaters...)
	 * @param statsKey                  The full stats key string :
	 *                                  appName/[modName/]class/method(params...)
	 */
	public void createRestMetric(String classMethodParamSignature, String statsKey);

	/**
	 * Update the REST metric with the duration value.
	 * 
	 * @param classMethodParamSignature The class and method signature:
	 *                                  class/method(paramaters...)
	 * @param statsKey                  The full stats key string :
	 *                                  appName/[modName/]class/method(params...)
	 * @param duration                  The duration timed from the filter
	 */
	public void updateRestMetric(String classMethodParamSignature, String statsKey, Duration duration);
}
