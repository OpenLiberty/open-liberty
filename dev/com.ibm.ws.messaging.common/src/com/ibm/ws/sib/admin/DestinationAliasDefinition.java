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

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.utils.SIBUuid8;

public interface DestinationAliasDefinition extends BaseDestinationDefinition {

    public final static int DEFAULT_DEFAULTPRIORITY = -1;

    /**
     * @return
     */
    public String getBus();

    /**
     * @param busName
     */
    public void setBus(String busName);

    /**
     * @return
     */
    public String getTargetName();

    /**
     * @param targetName
     */
    public void setTargetName(String targetName);

    /**
     * @return
     */
    public String getTargetBus();

    /**
     * @param targetBus
     */
    public void setTargetBus(String targetBus);

    /**
     * @return
     */
    public int getDefaultPriority();

    /**
     * @param arg
     */
    public void setDefaultPriority(int arg);

    /**
     * @return
     */
    public Reliability getMaxReliability();

    /**
     * @param arg
     */
    public void setMaxReliability(Reliability arg);

    /**
     * @return
     */
    public ExtendedBoolean isOverrideOfQOSByProducerAllowed();

    /**
     * @param arg
     */
    public void setOverrideOfQOSByProducerAllowed(ExtendedBoolean arg);

    /**
     * @return
     */
    public ExtendedBoolean isReceiveAllowed();

    /**
     * @param arg
     */
    public void setReceiveAllowed(ExtendedBoolean arg);

    /**
     * @return
     */
    public Reliability getDefaultReliability();

    /**
     * @param arg
     */
    public void setDefaultReliability(Reliability arg);

    /**
     * @return
     */
    public ExtendedBoolean isSendAllowed();

    /**
     * @param arg
     */
    public void setSendAllowed(ExtendedBoolean arg);

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
    public boolean getDelegateAuthorizationCheckToTarget();

    /**
     * @param arg
     */
    public void setDelegateAuthorizationCheckToTarget(boolean arg);

    /**
     * 
     * 
     * @return SIBuuid8[]
     */
    public SIBUuid8[] getScopedQueuePointMEs();

    /**
   * 
   *
   */
    public void setScopedQueuePointMEs(SIBUuid8[] arg);

}
