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

package com.ibm.ws.sib.ra.inbound;

/**
 * Type-safe enumeration for durable subscription sharing.
 */
public final class SibRaDurableSubscriptionSharing {

    
    /**
     * Always permit sharing.
     */
    public static final SibRaDurableSubscriptionSharing ALWAYS = new SibRaDurableSubscriptionSharing("ALWAYS");

    /**
     * Never permit sharing.
     */
    public static final SibRaDurableSubscriptionSharing NEVER = new SibRaDurableSubscriptionSharing("NEVER");

    /**
     * Only permit sharing in a cluster.
     */
    public static final SibRaDurableSubscriptionSharing CLUSTER_ONLY = new SibRaDurableSubscriptionSharing("CLUSTER_ONLY");

    /**
     * String representation of the shareability.
     */
    private final String _name;
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @param name a string representation of the shareability
     */
    private SibRaDurableSubscriptionSharing(final String name) {
        
        _name = name;
        
    }
    
    /**
     * Returns a string representation of this object.
     * 
     * @return a string representation of this object
     */
    public String toString() {
        
        return _name;
        
    }
    
}
