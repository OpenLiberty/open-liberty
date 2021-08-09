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

import com.ibm.websphere.sib.SIBDestinationReliabilityType;
import com.ibm.ws.sib.admin.AliasDestination;
import com.ibm.ws.sib.admin.SIBDestinationReliabilityInheritType;

/**
 * Represents the Destination of type Alias
 * 
 */
public class AliasDestinationImpl extends BaseDestinationImpl implements AliasDestination {

    private String targetDestination = null;
    private String overrideOfQOSByProducerAllowed = JsAdminConstants.TRUE;
    private String defaultReliability = SIBDestinationReliabilityType.ASSURED_PERSISTENT;
    private String maximumReliability = SIBDestinationReliabilityType.ASSURED_PERSISTENT;
    private String sendAllowed = JsAdminConstants.TRUE;
    private String receiveAllowed = JsAdminConstants.TRUE;
    private boolean delegateAuthCheckToTargetDestination = true;

    public AliasDestinationImpl() {
    // TODO Auto-generated constructor stub
    }

    public AliasDestinationImpl(String name) {
        // set isAlias to true and isLocal to false as we are creating AliasDestination
        super(name, true, false);
    }

    /**
     * An overridden method to compare two AliasDestination objects
     * 
     * @return boolean returns true if two objects are equal
     */
    @Override
    public boolean equals(Object o) {

        AliasDestination oldDestination = (AliasDestination) o;
        boolean isEqual = true;

        if (!this.getTargetDestination().equals(oldDestination.getTargetDestination())
                || !this.isOverrideOfQOSByProducerAllowed().equals(oldDestination.isOverrideOfQOSByProducerAllowed())

                || !this.isReceiveAllowed().equals(oldDestination.isReceiveAllowed())
                || !this.isSendAllowed().equals(oldDestination.isSendAllowed())
                || !this.getDefaultReliability().equals(oldDestination.getDefaultReliability())
                || !this.getMaximumReliability().equals(oldDestination.getMaximumReliability())
                || this.getDelegateAuthCheckToTargetDestination() != oldDestination.getDelegateAuthCheckToTargetDestination()) {
            isEqual = false;
        } else {// all the properties are equal
            isEqual = true;
        }

        return isEqual;
    }

    @Override
    public String getDefaultReliability() {
        return defaultReliability;
    }

    @Override
    public boolean getDelegateAuthCheckToTargetDestination() {
        return delegateAuthCheckToTargetDestination;
    }

    @Override
    public String getMaximumReliability() {
        return maximumReliability;
    }

    @Override
    public String getTargetDestination() {
        return targetDestination;
    }

    @Override
    public String isOverrideOfQOSByProducerAllowed() {
        return overrideOfQOSByProducerAllowed;
    }

    @Override
    public String isReceiveAllowed() {
        return receiveAllowed;
    }

    @Override
    public String isSendAllowed() {
        return sendAllowed;
    }

    @Override
    public void setDefaultReliability(String defaultReliability) {
        this.defaultReliability = getDestinationReliability(defaultReliability);

    }

    @Override
    public void setDelegateAuthCheckToTargetDestination(
                                                        boolean delegateAuthCheckToTargetDestination) {
        this.delegateAuthCheckToTargetDestination = delegateAuthCheckToTargetDestination;

    }

    @Override
    public void setMaximumReliability(String maximumReliability) {
        this.maximumReliability = getDestinationReliability(maximumReliability);

    }

    @Override
    public void setOverrideOfQOSByProducerAllowed(
                                                  String overrideOfQOSByProducerAllowed) {
        this.overrideOfQOSByProducerAllowed = overrideOfQOSByProducerAllowed;
    }

    @Override
    public void setReceiveAllowed(String receiveAllowed) {
        this.receiveAllowed = receiveAllowed;

    }

    @Override
    public void setSendAllowed(String sendAllowed) {
        this.sendAllowed = sendAllowed;

    }

    @Override
    public void setTargetDestination(String targetDestination) {
        this.targetDestination = targetDestination;

    }
    
    /**
	 * Returns the appropriate destination reliability based out of SIBDestinationReliabilityType
	 * @param reliability
	 * @return
	 */
	private String getDestinationReliability(String reliability){
		
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
		}else if(reliability.equals(JsAdminConstants.INHERIT)){
			rel = SIBDestinationReliabilityInheritType.INHERIT;
		}
		return rel;
	}

}
