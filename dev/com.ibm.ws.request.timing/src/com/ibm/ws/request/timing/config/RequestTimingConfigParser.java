/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.config;

import java.util.Dictionary;
import java.util.List;

/**
 * Processes the request timing configuration sub-elements and produces
 * Timing objects that are used to apply slow and hung request thresholds
 * to individual requests.
 */
public interface RequestTimingConfigParser {

	/**
	 * Returns the name of the configuration sub-element that can be processed
	 * by this config parser.
	 */
	public String getElementName();
	
	/**
	 * Process the configuration and generate Timing objects for any
	 * configuration sub-elements that are recognized by the parser.
	 */
	public TimingConfigGroup parseConfiguration(List<Dictionary<String, Object>> configElementList, long defaultSlowRequestThreshold, long defaultHungRequestThreshold, boolean defaultInterruptHungRequest);
}
