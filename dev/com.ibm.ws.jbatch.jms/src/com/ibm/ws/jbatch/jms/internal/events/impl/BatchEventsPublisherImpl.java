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

package com.ibm.ws.jbatch.jms.internal.events.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Set;

import javax.batch.runtime.BatchStatus;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.impl.BatchKernelImpl;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jbatch.jms.internal.BatchJmsMessageHelper;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;


/**
 * Provide JMS publishing service to batch component.
 * The list of current topics are defined in the BatchEvents interface.
 * This service is optional and is activated only when configuration
 * is available.
 * To configure this service, add batchJmsEvent element to server.xml and
 * appropriate JMS artifacts. For example:
 * 
 * <batchJmsEvents connectionFactoryRef="batchConnectionFactory" />
 * <jmsConnectionFactory id="batchConnectionFactory" jndiName="jms/batch/connectionFactory">
 *		<properties.wasJms></properties.wasJms>	
 * </jmsConnectionFactory>	
 *
 */
@Component(configurationPid = "com.ibm.ws.jbatch.jms.events",
configurationPolicy = ConfigurationPolicy.REQUIRE,
service = BatchEventsPublisher.class,
property = {"service.vendor=IBM"})
public class BatchEventsPublisherImpl implements BatchEventsPublisher {

	private static final TraceComponent tc = Tr.register(BatchEventsPublisherImpl.class, "wsbatch", "com.ibm.ws.jbatch.jms.internal.resources.BatchJmsMessages");

	/**
	 * For creating jms dispatcher connection factory
	 */
	private ResourceFactory jmsConnectionFactory;

	/**
	 * Resource configuration factory used to create a resource info object.
	 */
	private ResourceConfigFactory resourceConfigFactory;

	/**
	 * JMS event topic root to replace "batch".
	 * Replace the root of any event topic tree with 'topicRoot'.
	 * Keep the default root if 'topicRoot' is set to 'TOPIC_ROOT'.
	 */
	private String topicRoot = "";

	@Reference(service = ResourceConfigFactory.class)
	protected void setResourceConfigFactory(ResourceConfigFactory svc) {
		resourceConfigFactory = svc;
	}

	@Reference(target = "(id=unbound)")
	protected void setJMSConnectionFactory(ResourceFactory factory, Map<String, String> serviceProps) {
		jmsConnectionFactory = factory;
	}

	protected void unsetJmsConnectionFactory(ResourceFactory svc) {
		if (svc == jmsConnectionFactory) {
			jmsConnectionFactory = null;
		}
	}

	protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {
		if (svc == resourceConfigFactory) {
			resourceConfigFactory = null;
		}
	}


	/*
	 *Set 'topicRoot' or default is TOPIC_ROOT; 'batch'.
	 */
	public void setTopicRoot(String s) {

		//Note empty string removes 'batch' root from topic tree.
		topicRoot = (s!=null)? s : TOPIC_ROOT;
		Tr.info(tc, "info.batch.events.publish.topic", topicRoot + '/');

	}

	/*
	 * Create connection factory and topic
	 */
	@Activate
	protected void activate(ComponentContext context, Map<String, Object> config) throws Exception {
		setTopicRoot( (String) config.get("topicRoot") );
	}

	protected boolean deactivated = false;
	protected void deactivate() {
		deactivated = true;
	}

    private ConnectionFactory getConnectionFactory() {
        return getInitHelper().jmsCf;
    }
	private byte[] initHelperLock = new byte[0];

	// double-checked locking
	// http://www.oracle.com/technetwork/articles/javase/bloch-effective-08-qa-140880.html
	private volatile InitHelper initHelper;
	
	InitHelper getInitHelper() {
		InitHelper result = initHelper;
		if (result == null) { // First check (no locking)
			synchronized(initHelperLock) {
				result = initHelper;
				if (result == null) { // Second check (with locking)
					initHelper = result =  new InitHelper();
				}
			}
		}
		return result;
	}

	private class InitHelper {
		/**
		 * Connection factory for dispatch queue
		 */
		private ConnectionFactory jmsCf = null;
		
		private InitHelper() {
			initJMSResources();
		}

		private void initJMSResources() {
			try {
				ResourceConfig cfResourceConfig = resourceConfigFactory.createResourceConfig(ConnectionFactory.class.getName());
				cfResourceConfig.setResAuthType(ResourceInfo.AUTH_CONTAINER);
				jmsCf = (ConnectionFactory) jmsConnectionFactory.createResource(cfResourceConfig);

				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "        jmsCf = " + jmsCf.toString());
				}
			} catch (Exception e) {
				Tr.error(tc, "error.batch.events.publisher.jms.resource.activate", new Object[] { e });
				throw new RuntimeException(e);
			}
		}
	}


	/**
	 * Actually do the jms publishing with a correlation id on the message
	 * 
	 * @param jsonObj
	 * @param event
	 * @param correlationId May be null, in which case it won't be set on JMS message
	 */
	private void publishEventWithCorrelationId(JsonObject jsonObj, String event, String correlationId) {
		Connection topicConnection = null;
		Session topicSession = null;
		String topicName = null;

		//Removing 'contents' in trace record reduces trace log size by 10+ Megs.
		if (tc.isEntryEnabled()) {
			JsonObject trJsonObj = removeJsonPair( jsonObj, "contents");
			Tr.entry(tc, "publishEventWithCorrelationId", new Object[] { trJsonObj, event, correlationId} );
		}

		if (deactivated) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No-op; component deactivated = " + this);
			} 
		} else {

			//Get the updated or default topicRoot
			event = resolveTopicRoot( event );

			try {
				topicConnection = getConnectionFactory().createConnection();            
				topicSession = topicConnection.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

				TextMessage eventMsg = topicSession.createTextMessage();
				BatchJmsMessageHelper.setJobEventMessage(eventMsg, jsonObj);
				if (correlationId != null) {
					eventMsg.setJMSCorrelationID(correlationId);
				}
				Topic topicToPublish = topicSession.createTopic(event);
				MessageProducer publisher = topicSession.createProducer(topicToPublish);
				publisher.send(eventMsg);
				topicName = topicToPublish.getTopicName();

				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					StringBuffer msgBuf = new StringBuffer("topicName=" + topicName + " publisher destination=" + publisher.getDestination());           	
					if (correlationId != null) {	
						msgBuf.append(" published correlationId for instance id " + jsonObj.getJsonNumber("instanceId").longValue() + " = " + correlationId);
					}
					Tr.debug(tc,  msgBuf.toString()); 
				}

			} catch (JMSException ex) {
				Exception linkedException = ex.getLinkedException() != null ? ex.getLinkedException() : ex;
				Tr.warning(tc, "warning.batch.events.unable.to.publish", new Object[] {topicName, jsonObj.toString(), linkedException});            
			} finally {
				cleanUpJms(topicConnection, topicSession);
			}
		}

		if (tc.isEntryEnabled()) {
			Tr.exit(tc, "publishEventWithCorrelationId");
		}
	}

	/**
	 * Clean up jms objects
	 * @param myConn
	 * @param session
	 */
	private void cleanUpJms(Connection connection, Session session) {
		try {
			connection.close();
			session.close();
		} catch (Exception e) {
			//ffdc
		}
	}

	@Override
	public void publishPartitionEvent(int partitionNumber,
			BatchStatus batchStatus, String exitStatus, String stepName,
			long topLevelInstanceId, long topLevelExecutionId, long topLevelStepExecutionId,
			String eventToPublish, String correlationId) {

		JsonObject jsonObj = BatchJSONHelper.convertPartitionToJsonObjectBuilderForEvent(partitionNumber,
				batchStatus,
				exitStatus,
				stepName,
				topLevelInstanceId,
				topLevelExecutionId,
				topLevelStepExecutionId).build();

		publishEventWithCorrelationId(jsonObj, eventToPublish, correlationId);
	}

	@Override
	public void publishCheckpointEvent(String stepName, long jobInstanceId,
			long jobExecutionId, long stepExecutionId, String correlationId) {

		JsonObject jsonObj = BatchJSONHelper.convertCheckpointToJsonObjectBuilderForEvent(stepName,
				jobInstanceId,
				jobExecutionId,
				stepExecutionId).build();

		publishEventWithCorrelationId(jsonObj, BatchEventsPublisher.TOPIC_EXECUTION_STEP_CHECKPOINT, correlationId);
	}



	@Override
	public void publishSplitFlowEvent(String splitName, String flowName,
			long instanceId, long executionId,
			String splitFlowTopicString, String correlationId) {

		JsonObject jsonObj = BatchJSONHelper.convertSplitFlowToJsonObjectBuilderForEvent(splitName, flowName,
				instanceId,
				executionId).build();

		publishEventWithCorrelationId(jsonObj, splitFlowTopicString, correlationId);	
	}

	@Override
	public void publishJobInstanceEvent(WSJobInstance jobInstance,
			String event, String correlationId) {
		JsonObject jsonObj = BatchJSONHelper.toJsonObjectBuilder(jobInstance).build();
		publishEventWithCorrelationId(jsonObj, event,correlationId);

	}

	@Override
	public void publishJobExecutionEvent(WSJobExecution jobExecution,
			String event, String correlationId) {
		JsonObject jsonObj = BatchJSONHelper.toJsonObjectBuilderInBasicFormat(jobExecution).build();
		publishEventWithCorrelationId(jsonObj, event,correlationId);

	}

	@Override
	public void publishStepEvent(WSStepThreadExecutionAggregate objectToPublish,
			String event, String correlationId) {
		JsonObject jsonObj = BatchJSONHelper.convertStepExecutionToJsonObjectInBasicFormatNoLink((WSTopLevelStepExecution)objectToPublish.getTopLevelStepExecution(), null).build();
		publishEventWithCorrelationId(jsonObj, event, correlationId);  

	}

	@Override
	public void publishJobLogEvent(long topLevelInstanceId, long topLevelExecutionId, String appName,
			String stepName, Integer partitionNumber, String splitName,
			String flowName, int partNum, boolean finalLog, String jobLogContent, String correlationId) {

		String jobLogEvent = BatchEventsPublisher.TOPIC_JOB_LOG_PART;

		//Removing 'jobLogContent' in trace record reduces trace log size by 10+ Megs.
		if (tc.isEntryEnabled()) {
			Tr.entry(tc, "publishJobLogEvent", new Object[] { topLevelInstanceId, topLevelExecutionId, 
					appName, stepName, partitionNumber, splitName, flowName, partNum, finalLog,
					correlationId} );
		}

		JsonObject jsonObj;
		jsonObj = BatchJSONHelper.convertJobLogToJsonObjectBuilderForEvent(topLevelInstanceId,
				topLevelExecutionId,
				appName,
				partNum,
				partitionNumber,
				stepName,
				splitName,
				flowName,
				finalLog,
				jobLogContent).build();

		publishEventWithCorrelationId(jsonObj, jobLogEvent, correlationId);

		if (tc.isEntryEnabled()) {
			Tr.exit(tc, "publishJobLogEvent");
		}
	}

	//Return a copy of the json object with a k/v pair removed.
	@Trivial
	private JsonObject removeJsonPair( JsonObject jObj, String k ){
		JsonObjectBuilder builder = Json.createObjectBuilder();
		Set<Entry<String,JsonValue>> jSet = jObj.entrySet();

		for (Iterator<Entry<String, JsonValue>> iter = jSet.iterator(); iter.hasNext();) {
			Entry<String,JsonValue> e = (Entry<String, JsonValue>) iter.next();
			if ( e.getKey().compareTo(k) != 0){
				builder = builder.add(e.getKey(), e.getValue());
			}
		}

		return builder.build();
	}

	/*
	 *Return the batch events topic tree with root, 'topicRoot/tree', or the default 'TOPIC_ROOT/tree'. 
	 */
	@Override
	public String resolveTopicRoot( String eventString){
		if ( topicRoot != null && !topicRoot.equals(TOPIC_ROOT)) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "resolveTopicRoot",  "Non-default resolution with topicRoot = " + topicRoot);
			}
			return eventString.replaceFirst(TOPIC_ROOT + ((topicRoot.isEmpty())? "/":""), topicRoot);
		} else {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "resolveTopicRoot",  "Default resolution with topicRoot = " + topicRoot);
			}
			return eventString;
		}
	}
}
