/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin.internal;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIBDestinationReliabilityType;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.SIBDestination;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * Destination representing a Queue or Topic.The distinction is made by the attribute destinationType
 * topicAccessCheckRequired attribute is valid only for Topic and not Queue
 */
public class SIBDestinationImpl extends BaseDestinationImpl implements SIBDestination {

	/** RAS trace variable */
	private static final TraceComponent tc = SibTr.register(
			SIBDestinationImpl.class, JsConstants.TRGRP_AS,
			JsConstants.MSG_BUNDLE);
	
	private DestinationType destinationType = null;
	private boolean overrideOfQOSByProducerAllowed = true;
	private String defaultReliability = SIBDestinationReliabilityType.ASSURED_PERSISTENT;
	private String maximumReliability = SIBDestinationReliabilityType.ASSURED_PERSISTENT;
	private int defaultPriority = 0;
	private String exceptionDestination = JsAdminConstants.EXCEPTION_DESTINATION;
	private long blockedRetryTimeout = -1;
	private int maxFailedDeliveries = 5;
	private boolean sendAllowed = true;
	private boolean receiveAllowed = true;
	private boolean receiveExclusive = false;
	private boolean maintainStrictOrder = false;
	private long highMessageThreshold = 50000;
	private boolean persistRedeliveryCount = true;
	private String exceptionDiscardReliability = SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT;
	private boolean topicAccessCheckRequired = true;
        private String faileddeliveryPolicy="SEND_TO_EXCEPTION_DESTINATION";
	
	
	public SIBDestinationImpl() {
		super();
	}
	
	/**
	 * Used for creating defaults
	 * 
	 * @param name
	 * @param destinationType
	 */
	public SIBDestinationImpl(String name, DestinationType destinationType) {
		// set isAlias to false and isLocal to true as we are creating Destination of type Queue or Topic
		super(name,false,true);
		this.destinationType = destinationType;

	}
	
	
	/**
	 * An overridden method to compare two SIBDestination objects
	 */
	public boolean equals(Object o) {		
		SIBDestination oldDestination = (SIBDestination)o;
		boolean isEqual = true;
		
		if (isEqual(oldDestination))
		{   // all the properties are equal
			isEqual = true;
		}else{
			isEqual = false;
		}
		
		/**
		 * We have checked all the properties except topicaccesscheck which is applicable for TOPIC only.
		 * Now a last check is made to decide if two objects are equal or not
		 * if isEqual is false, we needn't check anything more as already two objects are not equal
		 */
		if(isEqual){ 
			if (this.getDestinationType().toString().equals(
					DestinationType.TOPICSPACE.toString())) {
				if (this.isTopicAccessCheckRequired() != oldDestination
						.isTopicAccessCheckRequired())
					isEqual = false;
			}
		}
		
		return isEqual;
		
	}


	private boolean isEqual(SIBDestination oldDest){

		if(this.isMaintainStrictOrder() != oldDest.isMaintainStrictOrder())
			return false;

		if(this.isOverrideOfQOSByProducerAllowed() != oldDest.isOverrideOfQOSByProducerAllowed())
			return false;
		
		if(this.isPersistRedeliveryCount() != oldDest.isPersistRedeliveryCount())
			return false;
		
		if(this.isReceiveAllowed() != oldDest.isReceiveAllowed())
			return false;
		
		if(this.isReceiveExclusive() != oldDest.isReceiveExclusive())
			return false;
		
		if(this.isSendAllowed() != oldDest.isSendAllowed())
			return false;
		
		if(this.getBlockedRetryTimeout() != oldDest.getBlockedRetryTimeout())
			return false;

		if(this.getDefaultPriority() != oldDest.getDefaultPriority())
			return false;
		
		if(!this.getDefaultReliability().equals(oldDest.getDefaultReliability()))
			return false;
		
		if((this.getExceptionDestination() == null ) && (oldDest.getExceptionDestination() == null)){
			//do nothing since both are equal
		}else if((this.getExceptionDestination() != null) && (oldDest.getExceptionDestination() == null)){
			return false;
		}else if((this.getExceptionDestination() == null) && (oldDest.getExceptionDestination() != null)){
			return false;
		}else if(!this.getExceptionDestination().equals(oldDest.getExceptionDestination())){
			return false;
		}
		
		if(!this.getExceptionDiscardReliability().equals(oldDest.getExceptionDiscardReliability()))
			return false;
		
		if(this.getHighMessageThreshold() != oldDest.getHighMessageThreshold())
			return false;
		
		if(this.getMaxFailedDeliveries() != oldDest	.getMaxFailedDeliveries())
			return false;
		
		if(!this.getMaximumReliability().equals(oldDest.getMaximumReliability()))
			return false;

		return true;
	}
	public DestinationType getDestinationType() {
		return destinationType;
	}

	public void setDestinationType(DestinationType destinationType) {
		this.destinationType = destinationType;
	}

	public boolean isOverrideOfQOSByProducerAllowed() {
		return overrideOfQOSByProducerAllowed;
	}

	public void setOverrideOfQOSByProducerAllowed(
			boolean overrideOfQOSByProducerAllowed) {
		this.overrideOfQOSByProducerAllowed = overrideOfQOSByProducerAllowed;
	}

	public String getDefaultReliability() {
		return defaultReliability;
	}

	public void setDefaultReliability(String defaultReliability) {
		
		this.defaultReliability = getDestinationReliability(defaultReliability);
	}

	public String getMaximumReliability() {
		return maximumReliability;
	}

	public void setMaximumReliability(String maximumReliability) {
		this.maximumReliability = getDestinationReliability(maximumReliability);
	}

	public int getDefaultPriority() {
		return defaultPriority;
	}

	public void setDefaultPriority(int defaultPriority) {
		this.defaultPriority = defaultPriority;
	}

	public String getExceptionDestination() {
		return exceptionDestination;
	}

	public void setExceptionDestination(String exceptionDestination) {
		this.exceptionDestination = exceptionDestination;
	}

	public long getBlockedRetryTimeout() {
		return blockedRetryTimeout;
	}

	public void setBlockedRetryTimeout(long blockedRetryTimeout) {
		this.blockedRetryTimeout = blockedRetryTimeout;
	}

	public int getMaxFailedDeliveries() {
		return maxFailedDeliveries;
	}

	public void setMaxFailedDeliveries(int maxFailedDeliveries) {
		this.maxFailedDeliveries = maxFailedDeliveries;
	}

	public boolean isSendAllowed() {
		return sendAllowed;
	}

	public void setSendAllowed(boolean sendAllowed) {
		this.sendAllowed = sendAllowed;
	}

	public boolean isReceiveAllowed() {
		return receiveAllowed;
	}

	public void setReceiveAllowed(boolean receiveAllowed) {
		this.receiveAllowed = receiveAllowed;
	}

	public boolean isReceiveExclusive() {
		return receiveExclusive;
	}

	public void setReceiveExclusive(boolean receiveExclusive) {
		this.receiveExclusive = receiveExclusive;
	}

	public boolean isMaintainStrictOrder() {
		return maintainStrictOrder;
	}

	public void setMaintainStrictOrder(boolean maintainStrictOrder) {
		this.maintainStrictOrder = maintainStrictOrder;
	}

	public long getHighMessageThreshold() {
		return highMessageThreshold;
	}

	public void setHighMessageThreshold(long highMessageThreshold) {
		this.highMessageThreshold = highMessageThreshold;
	}

	public boolean isPersistRedeliveryCount() {
		return persistRedeliveryCount;
	}

	public void setPersistRedeliveryCount(boolean persistRedeliveryCount) {
		this.persistRedeliveryCount = persistRedeliveryCount;
	}

	public String getExceptionDiscardReliability() {
		return exceptionDiscardReliability;
	}

	public void setExceptionDiscardReliability(
			String exceptionDiscardReliability) {
		this.exceptionDiscardReliability = getDestinationReliability(exceptionDiscardReliability);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isTopicAccessCheckRequired() {

		return topicAccessCheckRequired;
	}

	/** {@inheritDoc} */
	@Override
	public void setTopicAccessCheckRequired(boolean topicAccessCheckRequired) {
		this.topicAccessCheckRequired = topicAccessCheckRequired;

	}
	
	/**
	 * Returns the appropriate destination reliability based out of SIBDestinationReliabilityType
	 * @param reliability
	 * @return
	 */
	private String getDestinationReliability(String reliability){
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, "getDestinationReliability", new Object[] { reliability});
		}
		
		String rel = null;
		
		if(reliability.equals(JsAdminConstants.BESTEFFORTNONPERSISTENT)){
			rel = SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT;
		}else if(reliability.equals(JsAdminConstants.EXPRESSNONPERSISTENT)){
			rel = SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT;
		}else if(reliability.equals(JsAdminConstants.RELIABLENONPERSISTENT)){
			rel = SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT;
		}else if(reliability.equals(JsAdminConstants.RELIABLEPERSISTENT)){
			rel = SIBDestinationReliabilityType.RELIABLE_PERSISTENT;
		}else if(reliability.equals(JsAdminConstants.ASSUREDPERSISTENT)){
			rel = SIBDestinationReliabilityType.ASSURED_PERSISTENT;
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, "getDestinationReliability",new Object[]{rel});
		}
		
		return rel;
	}
	
	/** {@inheritDoc} */
	@Override
	public void setDefaultAndMaxReliability(String defaultReliability,String maxReliability,JsMEConfig oldJsmeConfig,boolean modified){
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, "setDefaultAndMaxRelaibility", new Object[] { defaultReliability,maxReliability,modified});
		}
		int iDefaultRel = getReliabilityInt(defaultReliability);
		int iMaxRel = getReliabilityInt(maxReliability);
		
		setDefaultReliability(defaultReliability);
		setMaximumReliability(maxReliability);
		
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, "setDefaultAndMaxRelaibility");
		}

	}
	
	private int getReliabilityInt(String reliability){
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.entry(tc, "getReliabilityInt", new Object[] { reliability});
		}
		
		int iRel = -1;
		
		if(reliability.equals(JsAdminConstants.BESTEFFORTNONPERSISTENT)){
			iRel = SIBDestinationReliabilityType.BEST_EFFORT_NONPERSISTENT_VALUE;
		}else if(reliability.equals(JsAdminConstants.EXPRESSNONPERSISTENT)){
			iRel = SIBDestinationReliabilityType.EXPRESS_NONPERSISTENT_VALUE;
		}else if(reliability.equals(JsAdminConstants.RELIABLENONPERSISTENT)){
			iRel = SIBDestinationReliabilityType.RELIABLE_NONPERSISTENT_VALUE;
		}else if(reliability.equals(JsAdminConstants.RELIABLEPERSISTENT)){
			iRel = SIBDestinationReliabilityType.RELIABLE_PERSISTENT_VALUE;
		}else if(reliability.equals(JsAdminConstants.ASSUREDPERSISTENT)){
			iRel = SIBDestinationReliabilityType.ASSURED_PERSISTENT_VALUE;
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			SibTr.exit(tc, "getReliabilityInt",new Object[]{iRel});
		}
		
		return iRel;
		
	}
	/**
	 * Set failed delivery policy 	
	 * @param failedDeliveryPolicy values can be SEND_TO_EXCEPTION_DESTINATION, DISCARD , KEEP_TRYING
	 */
	public void setFailedDeliveryPolicy(String failedDeliveryPolicy){
		this.faileddeliveryPolicy=failedDeliveryPolicy;
	}
	/**
	 * Get failed delivery policy
	 * the default value is SEND_TO_EXCEPTION_DESTINATION if nothing is set by user
	 */
        public String getFailedDeliveryPolicy(){
    	return this.faileddeliveryPolicy;
        }

}
