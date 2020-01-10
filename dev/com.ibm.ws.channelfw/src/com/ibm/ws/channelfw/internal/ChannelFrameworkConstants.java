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
package com.ibm.ws.channelfw.internal;

/**
 * This purpose of this interface is to consolidate Strings used throughout
 * the Channel Framework to prevent future changes from rippling to all
 * files.
 */
public interface ChannelFrameworkConstants {

    /** Trace group id used for the framework */
    String BASE_TRACE_NAME = "ChannelFramework";
    /** Resource bundle used for the framework */
    String BASE_BUNDLE = "com.ibm.ws.channelfw.internal.resources.ChannelfwMessages";

    /** Property name used by connector channels to store the hostname. */
    String HOST_NAME = "hostname";

    /**
     * Property name used by connector channels to store the port speicified in
     * the config.
     */
    String PORT = "port";

    /**
     * Property name used by connector channels to store the port actually being
     * used.
     */
    String LISTENING_PORT = "listeningPort";

    /**
     * Properies put into ChannelData property map when calling
     * ChannelFactory.findOrCreateChannel().
     */
    String CHAIN_DATA_KEY = "chainData";
    String CHAIN_NAME_KEY = "chainName";
}
