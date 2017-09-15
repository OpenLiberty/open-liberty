/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw;

import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.RegionType;

/**
 * Use this interface to indicate that your particular channel is bound to a
 * specific region within zOS.
 * 
 * Not implementing this will indicate that all channel chains should
 * attempt to be started in all regions, aka "no limits." This may
 * cause failures.
 * 
 * To use this interface simply implement this interface with your
 * WSChannelFactory.
 */
public interface BoundRegion {

    /**
     * Not started in the controller or control adjunct.
     */
    int NO_REGION = RegionType.NO_BOUND_REGION;
    /**
     * Bound to Controller or Control Region.
     */
    int CR_REGION = RegionType.CR_REGION;
    /**
     * Bound to Controller Adjuct or Control Region Adjunct.
     */
    int CRA_REGION = RegionType.CRA_REGION;

    /**
     * Get the region (CR_REGION or CRA_REGION) that this channel
     * based on this configuration is bound to. This is only valid
     * for Application Channels on the Inbound chains.
     * 
     * @param channelConfiguration
     * @return CR_REGION or CRA_REGION or NO_REGION
     */
    int getRegion(Map<String, ChannelData> channelConfiguration);

    /**
     * This will tell the framework that, if in the SR and based on this
     * particular configuration, whether or not to start the chains.
     * This is only valid for Application Channels on the Inbound chains.
     * 
     * @param channelConfiguration
     * @return true if we should start chains with this channel in the SR, false
     *         otherwise.
     */
    boolean isServantStartable(Map<String, ChannelData> channelConfiguration);

}