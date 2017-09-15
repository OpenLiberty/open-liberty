/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

import java.io.Serializable;
import java.util.Map;

/**
 * This interface includes methods to query details about an outbound channel
 * that can be used to talk to the inbound channel that provides this.
 */
public interface OutboundChannelDefinition extends Serializable {

    /**
     * Access method for the channel factory that is required to build the
     * outbound channel defined by this interface.
     * 
     * @return class of the channel factory
     */
    Class<?> getOutboundFactory();

    /**
     * Access the properties of the channel factory required to build the
     * outbound channel defined by this interface. If no properties are
     * necessary, return null.
     * 
     * @return map of channel factory properties
     */
    Map<Object, Object> getOutboundFactoryProperties();

    /**
     * Access the properties required by the outbound channel represented
     * by this interface. If no properties are necessary, return null.
     * 
     * Note that any String based properties are expected to either be English
     * or UTF-8 encoded, other encodings are not supported for configuration
     * values.
     * 
     * @return map of channel properties
     */
    Map<Object, Object> getOutboundChannelProperties();

}
