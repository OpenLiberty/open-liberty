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
//@(#) 1.1 SERV1/ws/code/channelfw/src/com/ibm/websphere/channel/framework/RegionType.java, WAS.channelfw, WASX.SERV1 8/20/04 10:39:41 [8/28/04 13:40:14]
package com.ibm.websphere.channelfw;

/**
 * The RegionType class is used among the entire framework to specify
 * different values related to the different Z regions.
 * 
 * @ibm-api
 */
public class RegionType {

    // -------------------------------------------------------------------------
    // Public Constants
    // -------------------------------------------------------------------------

    // These values need to be bit-wise exclusive
    // so binary logic can be used on variables which use these constants

    /**
     * Neither the CR_REGION or CRA_REGION, will be called "NO_BOUND_REGION".
     */
    public static final int NO_BOUND_REGION = 1;

    /**
     * Controller or Control Region.
     */
    public static final int CR_REGION = 2;

    /**
     * Adjuct or Control Region Adjunct.
     */
    public static final int CRA_REGION = 4;

    /**
     * Servant Region
     */
    public final static int SR_REGION = 8;

    /**
     * Not running on a Z platform
     */
    public final static int NOT_ON_Z = 16;

}
