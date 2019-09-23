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
package com.ibm.ws.request.timing.jdbc.internal;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.ws.request.timing.config.RequestTimingConfigParser;
import com.ibm.ws.request.timing.config.Timing;
import com.ibm.ws.request.timing.config.TimingConfigGroup;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", service = { RequestTimingConfigParser.class })
public class ConfigParser implements RequestTimingConfigParser {

    /** Name for JDBC timing element of Request Timing **/
    public static final String RT_JDBC_TIMING = "jdbcTiming";

    /** Name for the data source name in the jdbcTiming configuration **/
    public static final String RT_DATASOURCE_NAME = "dataSource";
    
    /** Name for the query name in the jdbcTiming configuration **/
    public static final String RT_QUERY_NAME = "query";
    
    /** The event type for servlet requests **/
  public static final String[] EVENT_TYPES = new String[] {
    "websphere.datasource.execute",
    "websphere.datasource.executeQuery",
    "websphere.datasource.executeUpdate",
    "websphere.datasource.psExecute",
    "websphere.datasource.psExecuteQuery",
    "websphere.datasource.psExecuteUpdate",
    "websphere.datasource.rsCancelRowUpdates",
    "websphere.datasource.rsDeleteRow",
    "websphere.datasource.rsInsertRow",
    "websphere.datasource.rsUpdateRow"};
    
	/** Trace service */
	private static final TraceComponent tc = Tr.register(ConfigParser.class);

    /**
     * Look for <servletTiming/> in the request timing configuration.
     */
	@Override
	public TimingConfigGroup parseConfiguration(List<Dictionary<String, Object>> configElementList, long defaultSlowRequestThreshold,
			long defaultHungRequestThreshold, boolean defaultInterruptHungRequest) {
		
		//Retrieve type specific settings
		//Check if the configuration contains the relevant attribute
		//If so assign the value of this attribute
		//Else assign the global value
		final List<Timing> slowRequestTimings = new LinkedList<Timing>();
		final List<Timing> hungRequestTimings = new LinkedList<Timing>();
		
		for (Dictionary<String, Object> configElement : configElementList) {
			try {			
				String pid = null;
				boolean typeInterruptHungRequest = false;
				long typeSlowReqThreshold = 0, typeHungReqThreshold = 0; 
				String[] contextInfo = new String[2];

				if(configElement.get(Constants.SERVICE_PID) != null) {
					pid = configElement.get(Constants.SERVICE_PID).toString();
				}
				
				if(configElement.get(RequestTimingConstants.RT_SLOW_REQUEST_THRESHOLD) != null){
					typeSlowReqThreshold = Long.parseLong(configElement.get(RequestTimingConstants.RT_SLOW_REQUEST_THRESHOLD).toString());
				}else{
					typeSlowReqThreshold = defaultSlowRequestThreshold;
				}

				if(configElement.get(RequestTimingConstants.RT_HUNG_REQUEST_THRESHOLD) != null){
					typeHungReqThreshold = Long.parseLong(configElement.get(RequestTimingConstants.RT_HUNG_REQUEST_THRESHOLD).toString());
				}else{
					typeHungReqThreshold = defaultHungRequestThreshold;
				}

				// See if a data source name or query name was supplied.
				Object dataSourceName = configElement.get(RT_DATASOURCE_NAME);
				Object queryName = configElement.get(RT_QUERY_NAME);

				// Construct a context info pattern around the values that were
				// supplied.
				if (dataSourceName != null) {
					contextInfo[0] = dataSourceName.toString();
				} else {
					contextInfo[0] = "*";
				}

				if (queryName != null) {
					contextInfo[1] = queryName.toString();
				} else {
					contextInfo[1] = "*";
				}

				// See if the request should be interrupted.
				if (configElement.get(RequestTimingConstants.RT_INTERRUPT_HUNG_REQUEST) != null) {
					typeInterruptHungRequest = Boolean.parseBoolean(configElement.get(RequestTimingConstants.RT_INTERRUPT_HUNG_REQUEST).toString());
				} else {
					typeInterruptHungRequest = defaultInterruptHungRequest;
				}

				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Nested timing element found", new Object[] {contextInfo, typeSlowReqThreshold, typeHungReqThreshold, typeInterruptHungRequest});
				}

				for (String type : EVENT_TYPES) {
					slowRequestTimings.add(new Timing(pid, type, contextInfo, typeSlowReqThreshold, false));
					hungRequestTimings.add(new Timing(pid, type, contextInfo, typeHungReqThreshold, typeInterruptHungRequest));
				}
			} catch (Exception e){
				// FFDC is injected here 
			}
		}

		return new TimingConfigGroup() {
			@Override
			public List<Timing> getSlowRequestTimings() {
				return slowRequestTimings;
			}

			@Override
			public List<Timing> getHungRequestTimings() {
				return hungRequestTimings;
			}
		};
	}

	@Override
	public String getElementName() {
		return RT_JDBC_TIMING;
	}
}
