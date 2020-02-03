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

package com.ibm.ws.request.timing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.request.timing.config.RequestTimingConfigParser;
import com.ibm.ws.request.timing.config.Timing;
import com.ibm.ws.request.timing.config.TimingConfigGroup;
import com.ibm.ws.request.timing.internal.config.HungRequestTimingConfig;
import com.ibm.ws.request.timing.internal.config.SlowRequestTimingConfig;
import com.ibm.ws.request.timing.notification.HungRequestNotification;
import com.ibm.ws.request.timing.probeExtensionImpl.HungRequestProbeExtension;
import com.ibm.ws.request.timing.probeExtensionImpl.SlowRequestProbeExtension;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;


public class RequestTimingService  implements ServerQuiesceListener {

	/** Trace service */
	private static final TraceComponent tc = Tr.register(RequestTimingService.class);

	/**
	 * The current copy of the configuration.
	 */
	private volatile Map<String, Object> currentConfig;
	
	/** 
	 * 	Store a reference to configuration admin.
	 *  Configuration admin will be used to retrieve timing values
	 *  for specific types.
	 */
	private volatile ConfigurationAdmin configAdmin = null; 

	private volatile SlowRequestProbeExtension slowRequestProbeExt = null;

	private volatile HungRequestProbeExtension hungRequestProbeExt = null;

	private String bundleLocation;

	private static volatile ScheduledExecutorService scheduledExecSrvc = null;
	
	private static volatile ExecutorService executorSrvc = null;

	private static volatile LibertyProcess libertyProcess = null;
	
	/** Registered hung request notifications **/
	private static volatile List<HungRequestNotification> hungRequestNotifications = 
			Collections.unmodifiableList(new ArrayList<HungRequestNotification>());
	
	/** Registered config parser.  These process <servletTiming/> and <jdbcTiming/>. */
	private static volatile List<RequestTimingConfigParser> configParsers =
			Collections.unmodifiableList(new ArrayList<RequestTimingConfigParser>());
	
	@Activate
	protected synchronized void activate(BundleContext ctx, Map<String, Object> configuration) {
		if(TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Activating " + this);
		}
		currentConfig = configuration;
		this.bundleLocation = ctx.getBundle().getLocation();
		configureService(false);
	}

	@Modified
	protected synchronized void modified(Map<String, Object> configuration) {
		if(TraceComponent.isAnyTracingEnabled() &&  tc.isEventEnabled()) {
			Tr.event(tc, " Modified " + this);
		}
		currentConfig = configuration;
		configureService(true);
	}

	@Deactivate
	protected synchronized void deactivate(int reason) {
		if(TraceComponent.isAnyTracingEnabled() &&  tc.isEventEnabled()) {
			Tr.event(tc, " Deactivating "+ this, " reason = " + reason);
		}
		this.bundleLocation = null;
		slowRequestProbeExt.stop();
		hungRequestProbeExt.stop();
		hungRequestNotifications = Collections.unmodifiableList(new ArrayList<HungRequestNotification>());
	}

	protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}

	protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = null;
	}

	protected void setScheduledExecutor(ScheduledExecutorService scheduledExecSrvc){
		RequestTimingService.scheduledExecSrvc = scheduledExecSrvc;
	}

	protected void unsetScheduledExecutor(ScheduledExecutorService scheduledExecSrvc){
		RequestTimingService.scheduledExecSrvc = null;
	}
	
	protected void setExecutor(ExecutorService executorSrvc){
		RequestTimingService.executorSrvc = executorSrvc;
	}

	protected void unsetExecutor(ExecutorService executorSrvc){
		RequestTimingService.executorSrvc = null;
	}
	

	protected void setLibertyProcess(LibertyProcess libertyProcess){
		RequestTimingService.libertyProcess = libertyProcess;
	}

	protected void unsetLibertyProcess(LibertyProcess libertyProcess){
		RequestTimingService.libertyProcess = null;
	}
	
	protected void setSlowRequestProbeExt(SlowRequestProbeExtension probeExt){
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting probe extension : "+ probeExt.getClass().getName());
		}
		slowRequestProbeExt = probeExt;
	}

	protected void unsetSlowRequestProbeExt(SlowRequestProbeExtension probeExt){
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Unsetting probe extension : "+ probeExt.getClass().getName());
		}
		slowRequestProbeExt = null;
	}

	protected void setHungRequestProbeExt(HungRequestProbeExtension probeExt){
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting probe extension : "+ probeExt.getClass().getName());
		}
		hungRequestProbeExt = probeExt;
	}

	protected void unsetHungRequestProbeExt(HungRequestProbeExtension probeExt){
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Unsetting probe extension : "+ probeExt.getClass().getName());
		}
		hungRequestProbeExt = null;
	}
	
	public synchronized void setHungRequestNotification(HungRequestNotification hungRequestNotification) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting the hung request notification into list", hungRequestNotification.getClass());
		}
		List<HungRequestNotification> notifications = new ArrayList<HungRequestNotification>(hungRequestNotifications);
		notifications.add(hungRequestNotification);
		hungRequestNotifications = Collections.unmodifiableList(notifications);
	}

	public synchronized void unsetHungRequestNotification(HungRequestNotification hungRequestNotification) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Removing hung request notification from list", hungRequestNotification.getClass());
		}
		List<HungRequestNotification> notifications = new ArrayList<HungRequestNotification>(hungRequestNotifications);
		notifications.remove(hungRequestNotification);
		hungRequestNotifications = Collections.unmodifiableList(notifications);
	}

	/*
	 * Note that this method needs to be synchronized with activate/modified because it
	 * forces the configuration to be re-read.
	 */
	protected synchronized void setConfigParser(RequestTimingConfigParser configParser) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting the config parser into list", configParser.getClass());
		}
		List<RequestTimingConfigParser> parsers = new ArrayList<RequestTimingConfigParser>(configParsers);
		parsers.add(configParser);
		configParsers = Collections.unmodifiableList(parsers);
		
		if (currentConfig != null) {
			configureService(true);
		}
	}

	/*
	 * Note that this method needs to be synchronized with activate/modified because it
	 * forces the configuration to be re-read.
	 */
	protected synchronized void unsetConfigParser(RequestTimingConfigParser configParser) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Removing config parser from list", configParser.getClass());
		}
		List<RequestTimingConfigParser> parsers = new ArrayList<RequestTimingConfigParser>(configParsers);
		parsers.remove(configParser);
		configParsers = Collections.unmodifiableList(parsers);
		
		if (currentConfig != null) {
			configureService(true);
		}
	}

	/**
	 * Used for updating configuration of the probe extensions
	 * @param modified : Should be set to true when being called from request timing
	 * 	service's modified(Map<String, Object> configuration) method.
	 */
	private void configureService(boolean modified) {

		//Initialize the local variables to default values.
		Map<String, Object> configuration = currentConfig;
		long slowRequestThreshold = RequestTimingConstants.SLOW_REQUEST_THRESHOLD_MS;
		long hungRequestThreshold = RequestTimingConstants.HUNG_REQUEST_THRESHOLD_MS;
		int sampleRate = RequestTimingConstants.SAMPLE_RATE;
		int contextInfoRequirement = ContextInfoRequirement.ALL_EVENTS;;
		boolean interruptHungRequest = false;
		Map<String, List<Timing>> slowRequestTiming = new HashMap<String, List<Timing>>();
		Map<String, List<Timing>> hungRequestTiming = new HashMap<String, List<Timing>>();

		//Retrieve global settings for request timing
		//Check if the configuration contains the relevant attribute
		//If so assign the value of this attribute
		//Else use the default value
		if(configuration.containsKey(RequestTimingConstants.RT_SAMPLE_RATE)){
			sampleRate = Integer.parseInt(configuration.get(RequestTimingConstants.RT_SAMPLE_RATE).toString());
			//Invalid input reset sampleRate to 1.
			if(sampleRate < 1)
				sampleRate = 1;
		}		

		if(configuration.containsKey(RequestTimingConstants.RT_INCLUDE_CONTEXT_INFO)){
			boolean includeContextInfo = Boolean.parseBoolean(configuration.get(RequestTimingConstants.RT_INCLUDE_CONTEXT_INFO).toString());

			if(includeContextInfo)
				contextInfoRequirement =  ContextInfoRequirement.ALL_EVENTS;
			else 
				contextInfoRequirement =  ContextInfoRequirement.NONE;
		}		

		if(configuration.containsKey(RequestTimingConstants.RT_SLOW_REQUEST_THRESHOLD))
			slowRequestThreshold = Long.parseLong(configuration.get(RequestTimingConstants.RT_SLOW_REQUEST_THRESHOLD).toString());


		if(configuration.containsKey(RequestTimingConstants.RT_HUNG_REQUEST_THRESHOLD))
			hungRequestThreshold = Long.parseLong(configuration.get(RequestTimingConstants.RT_HUNG_REQUEST_THRESHOLD).toString());

		if(configuration.containsKey(RequestTimingConstants.RT_INTERRUPT_HUNG_REQUEST)) 
			interruptHungRequest = Boolean.parseBoolean(configuration.get(RequestTimingConstants.RT_INTERRUPT_HUNG_REQUEST).toString());

		//Add the global settings to map.
		addToTimingSet(new Timing(RequestTimingConstants.ALL_TYPES, Timing.ALL_CONTEXT_INFO, slowRequestThreshold, false), slowRequestTiming);		
		addToTimingSet(new Timing(RequestTimingConstants.ALL_TYPES, Timing.ALL_CONTEXT_INFO, hungRequestThreshold, interruptHungRequest), hungRequestTiming);

		// Contact subtypes to add timing elements
		for (RequestTimingConfigParser parser : configParsers) {
			String subtype = parser.getElementName();
			String[] timingPids = (String[]) configuration.get(subtype);
			if (timingPids != null) {
				List<Dictionary<String, Object>> configElementList = new ArrayList<Dictionary<String, Object>>();

				for (String timingPid: timingPids) {
					try {				
						Configuration config = configAdmin.getConfiguration(timingPid, bundleLocation);
						Dictionary<String, Object> configElement = (config != null) ? config.getProperties() : null; 

						if (configElement == null) {
							// There was a problem reading the configuration.  
							// IE: CWWKG0058E: The element timing with the unique identifier default-1 is missing the required attribute eventType.
							Tr.error(tc, "REQUEST_TIMING_CONFIG_ERROR1", timingPid);
							continue;
						}

						configElementList.add(configElement);
					} catch (Exception e){
						// Instrumented trace/FFDC is disabled for this componenet.
						Object[] objs = new Object[] {this, timingPid};
						FFDCFilter.processException(e, this.getClass().getName(), "280", objs);
					}
				}

				TimingConfigGroup groupConfig = parser.parseConfiguration(configElementList, slowRequestThreshold, hungRequestThreshold, interruptHungRequest);
				List<String> conflictPids = new ArrayList<String>();
				for (Timing t : groupConfig.getSlowRequestTimings()) {
					checkForContextInfoConflict(conflictPids, contextInfoRequirement, t);
					addToTimingSet(t, slowRequestTiming);
				}
				for (Timing t : groupConfig.getHungRequestTimings()) {
					checkForContextInfoConflict(conflictPids, contextInfoRequirement, t);
					addToTimingSet(t, hungRequestTiming);
				}
			}
		}

		// Make the timing configuration read-only 
		SlowRequestTimingConfig slowReqTimingConfig = new SlowRequestTimingConfig(sampleRate, contextInfoRequirement, makeReadOnlyMap(slowRequestTiming));
		HungRequestTimingConfig hungReqTimingConfig = new HungRequestTimingConfig(contextInfoRequirement, makeReadOnlyMap(hungRequestTiming), interruptHungRequest);

		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Request timing configuration", slowReqTimingConfig, hungReqTimingConfig);
		}
		if(modified){
			slowRequestProbeExt.updateConfig(slowReqTimingConfig);
			hungRequestProbeExt.updateConfig(hungReqTimingConfig);
		}else{
			slowRequestProbeExt.setConfig(slowReqTimingConfig);
			hungRequestProbeExt.setConfig(hungReqTimingConfig);
		}
	}

	/**
	 * Make sure the context info requirement is appropriate for the timing element.  Specifically,
	 * that the context info will be supplied when the timing element requires us to match.
	 * @param conflictPids A set of PIDs which we already know are in error.
	 * @param contextInfoRequirement The current context info requirement.
	 * @param t The timing element we are checking.
	 */
	private void checkForContextInfoConflict(List<String> conflictPids, int contextInfoRequirement, Timing t) {
		if ((t.isDefaultTiming() == false) && (contextInfoRequirement != ContextInfoRequirement.ALL_EVENTS)) {
			String pid = t.getTimingPid();
			if (conflictPids.contains(pid) == false) {
				Tr.warning(tc, "REQUEST_TIMING_CONFIG_WARNING_CTX_INFO_PATTERN", pid);
			}
			if (pid != null) {
				conflictPids.add(pid);
			}
		}
	}

	private Map<String, List<Timing>> makeReadOnlyMap(Map<String, List<Timing>> timingsMap) {
		Map<String, List<Timing>> tempMap = new HashMap<String, List<Timing>>(timingsMap.size());
		for (String type : timingsMap.keySet()) {
			tempMap.put(type, Collections.unmodifiableList(timingsMap.get(type)));
		}
		return Collections.unmodifiableMap(tempMap);
	}
	
	private void addToTimingSet(Timing timing, Map<String, List<Timing>> requestTimingMap) {
		final String type = timing.getType();
		if (requestTimingMap.containsKey(type)) {
			List<Timing> timingForType = requestTimingMap.get(type);
			// TODO: Old map code checked for duplicates, can we do that here?
			timingForType.add(timing);
		} else {
			List<Timing> timingForType = new ArrayList<Timing>();
			timingForType.add(timing);
			requestTimingMap.put(type, timingForType);
		}
	}

	public static ScheduledExecutorService getScheduledExecutorService(){
		return scheduledExecSrvc;
	}
	
	public static ExecutorService getExecutorService(){
		return executorSrvc;
	}
	
	public static LibertyProcess getLibertyProcess(){
		return libertyProcess;
	}
	
	public static List<HungRequestNotification> getHungRequestNotifications() {
		return hungRequestNotifications;
	}
	
	/**
	 * Iterate through all the hung request notifications and
	 * tell them that a hung request was detected.
	 * 
	 * @param requestId : request id of the hung request
	 * @parm threadId : thread id of the thread where the hung request was running 
	 */
	public static void processAllHungRequestNotifications(String requestId, long threadId) {
		List<HungRequestNotification> hungRequestNotificationList = getHungRequestNotifications();
		for (int i = 0; i < hungRequestNotificationList.size(); i ++) {
			HungRequestNotification hungRequestNotification = hungRequestNotificationList.get(i);
			hungRequestNotification.hungRequestDetected(requestId, threadId);
		}
	}

	@Override
	public void serverStopping() {
		slowRequestProbeExt.stop();
        hungRequestProbeExt.stop();
		
	}
}
