/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * 
 * 
 *
 * Change activity:
 *
 * Reason          Date   Origin   Description
 * --------------- ------ -------- --------------------------------------------
 * SIB0211.adm.1   260107 leonarda New
 * SIB0211.adm.2   020307 leonarda Add configId
 * ============================================================================
 */
package com.ibm.ws.sib.admin;

import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * @author leonarda
 * 
 *         This class is a wrapper for the MQLink configuration and its
 *         sub-objects.
 * 
 */
public interface MQLinkDefinition {

    /**
     * Get the configId WCCM refId
     * 
     * @return String configId
     */
    public String getConfigId();

    /**
     * Get the UUID
     * 
     * @return SIBUuid8 containing the UUID of the link
     */
    public SIBUuid8 getUuid();

    /**
     * Get the Name
     * 
     * @return string containing the name of the link
     */
    public String getName();

    /**
     * Get the description
     * 
     * @return string description
     */
    public String getDescription();

    /**
     * Get the targetUuid
     * 
     * @return string target uuid of the SIBVirtualLink
     */
    public String getTargetUuid();

    /**
     * Get the Queue manager name
     * 
     * @return string Queue manager name
     */
    public String getQmName();

    /**
     * Get the batch size
     * 
     * @return int batch size
     */
    public int getBatchSize();

    /**
     * Get the max message size
     * 
     * @return int max message size
     */
    public int getMaxMsgSize();

    /**
     * Get the heartbeat interval
     * 
     * @return int hearbeat
     */
    public int getHeartBeat();

    /**
     * Get the sequence wrap
     * 
     * @return long sequence wrap
     */
    public long getSequenceWrap();

    /**
     * Get the non-persistent message speed
     * 
     * @return String non-persistent message speed
     *         CT_SIBMQLinkNPMSpeedType.FAST |
     *         CT_SIBMQLinkNPMSpeedType.NORMAL
     */
    public String getNpmSpeed();

    /**
     * Get the adoptable
     * 
     * @return boolean adoptable
     */
    public boolean getAdoptable();

    /**
     * Get the initial state
     * 
     * @return String initial state
     *         CT_SIBMQLinkInitialState.STOPPED |
     *         CT_SIBMQLinkInitialState.STARTED
     */
    public String getInitialState();

    /**
     * Get the sender channel definition
     * 
     * @return MQLinkSenderChannelDefinition sender channel (null if
     *         not defined)
     */
    public MQLinkSenderChannelDefinition getSenderChannel();

    /**
     * Get the receiver channel definition
     * 
     * @return MQLinkReceiverChannelDefinition receiver channel
     *         (null if not defined)
     */
    public MQLinkReceiverChannelDefinition getReceiverChannel();

}
