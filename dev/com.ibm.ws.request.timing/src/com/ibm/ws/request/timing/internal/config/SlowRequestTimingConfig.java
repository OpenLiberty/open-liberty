/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.request.timing.internal.config;

import java.util.List;
import java.util.Map;

import com.ibm.ws.request.timing.config.Timing;

public class SlowRequestTimingConfig extends RequestTimingConfig {

	public SlowRequestTimingConfig(int sampleRate, int contextInfoRequirement, Map<String, List<Timing>> slowRequestTiming){
		super(sampleRate, contextInfoRequirement, slowRequestTiming);
	}
	
	public SlowRequestTimingConfig() {
		super();
	}

	@Override
	public String toString() {
		StringBuffer slowReqTimingCfg = new StringBuffer();
		slowReqTimingCfg.append(String.format("%n"));
		slowReqTimingCfg.append("-------------------Slow Request Timing Settings-------------------" + String.format("%n"));
		slowReqTimingCfg.append("Sample rate: " + getSampleRate() + String.format("%n"));
		slowReqTimingCfg.append("Context info requirement: " + getContextInfoRequirement() + String.format("%n"));
		slowReqTimingCfg.append("-------------------Type Settings-------------------" + String.format("%n"));
		for(List<Timing> typeList : getRequestTiming().values()) {
			for (Timing t : typeList) {
				slowReqTimingCfg.append(t.getType() + ": " + t.getContextInfoString() + ": " + "Request threshold (ms) - " +  t.getRequestThreshold() + String.format("%n"));
			}
		}
		slowReqTimingCfg.append("-------------------------------------------------------------");
		return slowReqTimingCfg.toString();
	}
}
