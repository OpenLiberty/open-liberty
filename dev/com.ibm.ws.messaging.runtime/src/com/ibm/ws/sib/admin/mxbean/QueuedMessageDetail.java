/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin.mxbean;

import java.beans.ConstructorProperties;

public class QueuedMessageDetail extends QueuedMessage {

	private String _type = null;

	// Data members of SIBusMessage
	private String _bus_discriminator;
	private Integer _bus_priority;
	private String _bus_reliability;
	private Long _bus_timeToLive;
	private String _bus_replyDiscriminator;
	private Integer _bus_replyPriority;
	private String _bus_replyReliability;
	private Long _bus_replyTimeToLive;
	private String _bus_systemMessageId;
	private Long _bus_exceptionTimestamp;
	private String _bus_exceptionMessage;
	private String _bus_exceptionProblemSubscription;
	private String _bus_exceptionProblemDestination;

	// Data members of JsMessage
	private String _js_messageType;
	private int _js_approximateLength;
	private Long _js_timestamp;
	private Long _js_messageWaitTime;
	private Long _js_currentMEArrivalTimestamp;
	private Integer _js_redeliveredCount;
	private String _js_securityUserid;
	private String _js_producerType;
	private byte[] _js_apiMessageIdAsBytes;
	private byte[] _js_correlationIdAsBytes;

	// Data members of JsApiMessage
	private String _api_messageId;
	private String _api_correlationId;
	private String _api_userid;
	private String _api_format;

	// Data members of JsJmsMessage
	private String _jms_deliveryMode;
	private Long _jms_expiration;
	private String _jms_destination;
	private String _jms_replyTo;
	private Boolean _jms_redelivered;
	private String _jms_type;
	private int _jms_xDeliveryCount;
	private String _jms_xAppId;

	@ConstructorProperties({ "type", "jmsxAppId", "jmsxDeliveryCount",
			"apiCorrelationId", "apiFormat", "apiMessageId", "apiUserId",
			"jsApproximateLength", "jsMessageType", "jsProducerType",
			"jsSecurityUserId", "jsCurrrentMEArrivalTimestamp",
			"jsMessageWaitTime", "jsRedeliveredCount", "jsTimestamp", "state",
			"busPriority", "busTimeToLive" })
	public QueuedMessageDetail(String id, String name, String state,
			long timestamp, String transactionId, String type,
			String busDiscriminator, Integer busPriority,
			String busReliability, Long busTimeToLive,
			String busReplyDiscriminator, Integer busReplyPriority,
			String busReplyReliability, Long busReplyTimeToLive,
			String busSystemMessageId, Long busExceptionTimestamp,
			String busExceptionMessage, String busExceptionProblemSubscription,
			String busExceptionProblemDestination, String jsMessageType,
			int jsApproximateLength, Long jsTimestamp, Long jsMessageWaitTime,
			Long jsCurrentMEArrivalTimestamp, Integer jsRedeliveredCount,
			String jsSecurityUserid, String jsProducerType,
			byte[] jsApiMessageIdAsBytes, byte[] jsCorrelationIdAsBytes,
			String apiMessageId, String apiCorrelationId, String apiUserid,
			String apiFormat, String jmsDeliveryMode, Long jmsExpiration,
			String jmsDestination, String jmsReplyTo, Boolean jmsRedelivered,
			String jmsType, int jmsXDeliveryCount, String jmsXAppId,
			boolean fullyValid, boolean valid) {

		super(id, name, jsApproximateLength, state, transactionId, type,
				busSystemMessageId);

		this._type = type;

		// Data members of SIBusMessage
		this._bus_discriminator = busDiscriminator;
		this._bus_priority = busPriority;
		this._bus_reliability = busReliability;
		this._bus_timeToLive = busTimeToLive;
		this._bus_replyDiscriminator = busReplyDiscriminator;
		this._bus_replyPriority = busReplyPriority;
		this._bus_replyReliability = busReliability;
		this._bus_replyTimeToLive = busReplyTimeToLive;
		this._bus_systemMessageId = busSystemMessageId;
		this._bus_exceptionTimestamp = busExceptionTimestamp;
		this._bus_exceptionMessage = busExceptionMessage;
		// F001333-14609
		this._bus_exceptionProblemSubscription = busExceptionProblemSubscription;
		this._bus_exceptionProblemDestination = busExceptionProblemDestination;

		// Data members of JsMessage
		this._js_messageType = jsMessageType;
		this._js_approximateLength = jsApproximateLength;
		this._js_timestamp = jsTimestamp;
		this._js_messageWaitTime = jsMessageWaitTime;
		this._js_currentMEArrivalTimestamp = jsCurrentMEArrivalTimestamp;
		this._js_redeliveredCount = jsRedeliveredCount;
		this._js_securityUserid = jsSecurityUserid;
		this._js_producerType = jsProducerType;
		this._js_apiMessageIdAsBytes = jsApiMessageIdAsBytes;
		this._js_correlationIdAsBytes = jsCorrelationIdAsBytes;

		// Data members of JsApiMessage
		this._api_messageId = apiMessageId;
		this._api_correlationId = apiCorrelationId;
		this._api_userid = apiUserid;
		this._api_format = apiFormat;

		// Data members of JsJmsMessage
		this._jms_deliveryMode = jmsDeliveryMode;
		this._jms_expiration = jmsExpiration;
		this._jms_destination = jmsDestination;
		this._jms_replyTo = jmsReplyTo;
		this._jms_redelivered = jmsRedelivered;
		this._jms_type = jmsType;
		this._jms_xDeliveryCount = jmsXDeliveryCount;
		this._jms_xAppId = jmsXAppId;
	}

	public String getType() {
		return _type;
	}

	// Attributes of SIBusMessage

	public String getBusDiscriminator() {
		return _bus_discriminator;
	}

	public Integer getBusPriority() {
		return _bus_priority;
	}

	public String getBusReliability() {
		return _bus_reliability;
	}

	public Long getBusTimeToLive() {
		return _bus_timeToLive;
	}

	public String getBusReplyDiscriminator() {
		return _bus_replyDiscriminator;
	}

	public Integer getBusReplyPriority() {
		return _bus_replyPriority;
	}

	public String getBusReplyReliability() {
		return _bus_replyReliability;
	}

	public Long getBusReplyTimeToLive() {
		return _bus_replyTimeToLive;
	}

	public String getBusSystemMessageId() {
		return _bus_systemMessageId;
	}

	// Attributes of JsMessage

	public String getJsMessageType() {
		return _js_messageType;
	}

	public int getJsApproximateLength() {
		return _js_approximateLength;
	}

	public Long getJsTimestamp() {
		return _js_timestamp;
	}

	public Long getJsMessageWaitTime() {
		return new Long(updateMessageWaitTime());
	}

	public Long getJsCurrentMEArrivalTimestamp() {
		return _js_currentMEArrivalTimestamp;
	}

	public Integer getJsRedeliveredCount() {
		return _js_redeliveredCount;
	}

	public String getJsSecurityUserid() {
		return _js_securityUserid;
	}

	public String getJsProducerType() {
		return _js_producerType;
	}

	public byte[] getJsApiMessageIdAsBytes() {
		return _js_apiMessageIdAsBytes;
	}

	public byte[] getJsCorrelationIdAsBytes() {
		return _js_correlationIdAsBytes;
	}

	// Attributes of JsApiMessage

	public String getApiMessageId() {
		return _api_messageId;
	}

	public String getApiCorrelationId() {
		return _api_correlationId;
	}

	public String getApiUserid() {
		return _api_userid;
	}

	public String getApiFormat() {
		return _api_format;
	}

	// Attributes of JsJmsMessage

	public String getJmsDeliveryMode() {
		return _jms_deliveryMode;
	}

	public Long getJmsExpiration() {
		return _jms_expiration;
	}

	public String getJmsDestination() {
		return _jms_destination;
	}

	public String getJmsReplyTo() {
		return _jms_replyTo;
	}

	public Boolean getJmsRedelivered() {
		return _jms_redelivered;
	}

	public String getJmsType() {
		return _jms_type;
	}

	public int getJmsxDeliveryCount() {
		return _jms_xDeliveryCount;
	}

	public String getJmsxAppId() {
		return _jms_xAppId;
	}

	//

	public Long getExceptionTimestamp() {
		return _bus_exceptionTimestamp;
	}

	public String getExceptionMessage() {
		return _bus_exceptionMessage;
	}

	public String getExceptionProblemSubscription() {
		return _bus_exceptionProblemSubscription;
	}

	public String getExceptionProblemDestination() {
		return _bus_exceptionProblemDestination;
	}

	/**
	 * Update the message wait time
	 * 
	 * @return
	 */
	private long updateMessageWaitTime() {
		// Store in the message the amount of time it was on the queue
		long timeNow = java.lang.System.currentTimeMillis();
		long latestWaitTimeUpdate = calculateWaitTimeUpdate(timeNow);
		long messageWaitTime = latestWaitTimeUpdate
				+ _js_messageWaitTime.longValue();

		return messageWaitTime;
	}

	/**
	 * Calculate the current wait time.
	 * 
	 * @param timeNow
	 * @return
	 */
	private long calculateWaitTimeUpdate(long timeNow) {
		long calculatedWaitTime = timeNow
				- _js_currentMEArrivalTimestamp.longValue();

		return calculatedWaitTime;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer("JMS MESSAGE PROPERTIES : \n");
		buffer.append(": JMS delivery mode = " + this._jms_deliveryMode);
		buffer.append(": JMS type = " + this._jms_type);
		buffer.append(": JMSX application ID = " + this._jms_xAppId);
		buffer.append(": JMSX delivery count = " + this._jms_xDeliveryCount
				+ "\n");

		buffer.append("\n API MESSAGE PROPERTIES : \n");
		buffer.append(": Correlation Id = " + this._api_correlationId);// Correlation
																		// ID
		buffer.append(": FORMAT = " + this._api_format);
		buffer.append(": Message Id= " + this._api_messageId);
		buffer.append(": User Id= " + this._api_userid + "\n");

		buffer.append("\n MESSAGE PROPERTIES : \n");
		buffer.append(": Approximate length = " + this._js_approximateLength);
		buffer.append(": Message type = " + this._js_messageType);
		buffer.append(": Producer type = " + this._js_producerType);
		buffer.append(": Security user ID = " + this._js_securityUserid);
		buffer.append(": Current messaging engine arrival time = "
				+ this._js_currentMEArrivalTimestamp);
		buffer.append(": Message wait time = " + this._js_messageWaitTime);
		buffer.append(": Redelivered count = " + this._js_redeliveredCount);
		buffer.append(": Time stamp = " + this._js_timestamp);
		buffer.append(": State = " + this.getState());
		buffer.append(": Message priority= " + this._bus_priority);
		buffer.append(": Message Time To Live = " + this._bus_timeToLive);
		return buffer.toString();
	}

}
