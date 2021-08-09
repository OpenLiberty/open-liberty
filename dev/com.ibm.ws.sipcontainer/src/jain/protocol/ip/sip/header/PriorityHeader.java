/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

/**
 * <p>
 * This interface represents the Priority request-header.
 * The PriorityHeader indicates the urgency of the
 * Request as perceived by the client.
 * </p><p>
 * The currently defined priority values are:
 * </p>
 * <UL>
 * 	<LI> PRIORITY_EMERGENCY
 * 	<LI> PRIORITY_URGENT
 * 	<LI> PRIORITY_NORMAL
 * 	<LI> PRIORITY_NON_URGENT
 * </UL>
 * <p>
 * It is recommended that the value of PRIORITY_EMERGENCY
 * only be used when life, limb or property are in imminent danger.
 * </p>
 *
 * @version 1.0
 *
 */
public interface PriorityHeader extends Header
{
    
    /**
     * Set priority of PriorityHeader
     * @param <var>priority</var> priority
     * @throws IllegalArgumentException if priority is null
     * @throws SipParseException if priority is not accepted by implementation
     */
    public void setPriority(String priority)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets priority of PriorityHeader
     * @return priority of PriorityHeader
     */
    public String getPriority();
    
    /**
     * Urgent priority constant
     */
    public static final String PRIORITY_URGENT = "urgent";
    
    /**
     * Normal priority constant
     */
    public static final String PRIORITY_NORMAL = "normal";
    
    /**
     * Non-urgent priority constant
     */
    public static final String PRIORITY_NON_URGENT = "non-urgent";
    
    /**
     * Name of PriorityHeader
     */
    public final static String name = "Priority";
    
    //////////////////////////////////////////////////////////////////////
    
    /**
     * Emergency priority constant
     */
    public static final String PRIORITY_EMERGENCY = "emergency";
}
