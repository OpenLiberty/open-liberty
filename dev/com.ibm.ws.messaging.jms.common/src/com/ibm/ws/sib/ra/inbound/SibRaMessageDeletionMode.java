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
 * Type-safe enumeration for deletion modes.
 */
public final class SibRaMessageDeletionMode {

    
    /**
     * Deletion mode where the resource adapter deletes each message
     * individually after delivery.
     */
    public static final SibRaMessageDeletionMode SINGLE = new SibRaMessageDeletionMode("SINGLE");

    /**
     * Deletion mode where the resource adapter may deliver a batch of messages
     * and then delete them all together. This may represent a performance
     * improvement but increases the window during which the server may crash
     * and a message may be redelivered.
     */
    public static final SibRaMessageDeletionMode BATCH = new SibRaMessageDeletionMode("BATCH");

    /**
     * Deletion mode where the resource adapter does not delete the messages
     * after delivery instead leaving this to the application.
     */
    public static final SibRaMessageDeletionMode APPLICATION = new SibRaMessageDeletionMode("APPLICATION");

    /**
     * String representation of the message deletion mode.
     */
    private final String _name;
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @param name a string representation of the mode
     */
    private SibRaMessageDeletionMode(final String name) {
        
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
