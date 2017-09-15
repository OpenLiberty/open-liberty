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

package com.ibm.ws.sib.admin;

import java.util.Map;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.wsspi.sib.core.DestinationType;

// This comment by Jamie?
// Since DestinationDefinition represents a logical bus-wide entity, it does
// not make sense to permit operational MBean updates (which are ME-scoped in
// the current WAS architecture). The set methods below therefore identify 
// allowable configuration changes. Those attributes that do not have a set
// method cannot be changed without changing the identity of the destination
// (i.e. creating a new one with a new UUID).

// PRC Comment:
// I see no real need for setAttribute() calls other than to support dynamic
// update. Is this correct?
// A. setAttribute calls will be useful for unit testing purposes, and may
// have a part to play in some dynamic update cases?

public interface DestinationDefinition extends BaseDestinationDefinition {

    public long getAlterationTime();

    public void setAlterationTime(long l);

    public long getBlockedRetryTimeout();

    public void setBlockedRetryTimeout(long l);

    // PRC Comment:  Who is moving this? Will it be have to be done at the same time?  
    // DestinationType from SIB.core, which needs to:
    // (i)  be moved to replace the one in SIB.common
    // (ii) add discrimination between temporary queues and topicspaces
    // Was called getType
    // You cannot change a destination's DestinationType once created - a new 
    // destination with a new UUID must be created	
    public DestinationType getDestinationType();

    /**
     * Is this destination mediated? If so, then a valid MediationDefinition will be returned,
     * otherwise null indicates the destination is not mediated.
     * 
     * @return MediationDefinition the definition for the mediation that is applied to this
     *         destination, or null if the destination is not mediated.
     */
//  public MediationDefinition getMediationDefinition();

    /**
     * Indicate that this destination is mediated.
     * 
     * @param arg the MediationDefinition for the mediation which is being applied to this
     *            destination.
     */
//  public void setMediationDefinition(MediationDefinition arg);

    /**
     * If the destination is mediated, then is the mediation of this destination enabled in the
     * configuration? The return value from this method is only applicable if a MediationDefinition
     * is returned by the getMediationDefinition() method.
     * 
     * @return boolean whether the destination mediation is enabled (true) or disabled (false)
     */
//  public boolean isMediationEnabled();

    /**
     * Set the enabled state of the destination mediation.
     * 
     * @param arg is set to true to indicate the destination mediation is enabled or false to
     *            indicate it is disabled.
     */
//  public void setMediationEnabled(boolean arg);

    public Map getDestinationContext();

    public void setDestinationContext(Map arg);

    // Following are inhibit/quiesce-related properties that are the subject of
    // design issue 57
    public boolean isSendAllowed();

    public void setSendAllowed(boolean arg);

    public boolean isReceiveAllowed();

    public void setReceiveAllowed(boolean arg);

    public boolean isReceiveExclusive();

    public void setReceiveExclusive(boolean arg);

    public int getDefaultPriority();

    public void setDefaultPriority(int arg);

    public String getExceptionDestination();

    public void setExceptionDestination(String arg);

    public int getMaxFailedDeliveries();

    public void setMaxFailedDeliveries(int arg);

    public boolean isRedeliveryCountPersisted();

    public void setRedeliveryCountPersisted(boolean arg);

    public boolean isOverrideOfQOSByProducerAllowed();

    public void setOverrideOfQOSByProducerAllowed(boolean arg);

    public Reliability getDefaultReliability();

    public void setDefaultReliability(Reliability arg);

    // Allows an administrator to restrict reliability used to, for example, only
    // those that are not such that he has to back up the message store 
    // An exception is thrown on send if isProducerQOSOverrideEnabled returns true 
    // and the app exceeds the maxReliability
    public Reliability getMaxReliability();

    public void setMaxReliability(Reliability arg);

    /**
     * Return the configured reply destination for this destination.
     * 
     * @return
     */
    public QualifiedDestinationName getReplyDestination();

    /**
     * Return an array containing forward any routing path entries for this destination.
     * This method is only relevant if the DestinationType is QUEUE.
     * 
     * @return
     */
    public QualifiedDestinationName[] getForwardRoutingPath();

    /**
     * @return
     */
    public boolean isTopicAccessCheckRequired();

    /**
     * @param arg
     */
    public void setTopicAccessCheckRequired(boolean arg);

    /**
     * @return boolean orderingRequired
     */
    public boolean isOrderingRequired();

    /**
     * @param boolean orderingRequired
     */
    public void maintainMsgOrder(boolean arg);

    /**
     * @return boolean isAuditAllowed
     */
    public boolean isAuditAllowed();

    /**
     * Get the reliability level at which exceptioned messages on a destination will
     * automatically be discarded. The default level is BEST_EFFORT_NONPERSISTENT.
     * 
     * @return Reliability
     */
    public Reliability getExceptionDiscardReliability();

    /**
     * Set the reliability level at which exceptioned messages on a destination will
     * automatically be discarded.
     * 
     * @param exceptionDiscardReliability
     */
    public void setExceptionDiscardReliability(Reliability exceptionDiscardReliability);
    
    //Venu Liberty change
    /**
     * Get UUID of destination
    */
    public SIBUuid12 getUUID();

}
