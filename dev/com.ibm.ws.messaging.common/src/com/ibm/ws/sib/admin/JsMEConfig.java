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

import java.io.Serializable;

/**
 * An object representing the messaging engine properties sent by the Configuration Admin
 *
 */
public class JsMEConfig implements Serializable,LWMConfig {

    private static final long serialVersionUID = 3771927699307638584L;

    // Object to hold the filestore properties
    private SIBFileStore sibFilestore;
    // Object to hold some of the ME specific properties 
    private SIBMessagingEngine messagingEngine;
    // A default bus for the ME
    private SIBus sibus;

    /**
     * Get the filestore object
     * @return SIBFileStore
     */
    public SIBFileStore getSibFilestore() {
        return sibFilestore;
    }

    /**
     * Set the filestore object
     * @param sibFilestore
     */
    public void setSIBFilestore(SIBFileStore sibFilestore) {
        this.sibFilestore = sibFilestore;
    }

    /**
     * Get the SIBMessagingEngine object
     * @return
     */
    public SIBMessagingEngine getMessagingEngine() {
        return messagingEngine;
    }

    /**
     * Set the messaging engine object
     * @param messagingEngine
     */
    public void setMessagingEngine(SIBMessagingEngine messagingEngine) {
        this.messagingEngine = messagingEngine;
    }

    /**
     * Get the default bus
     * @return
     */
    
    public SIBus getSIBus() {
        return sibus;
    }

    /**
     * Set the default bus 
     * @param sibus
     */
    public void setSIBus(SIBus sibus) {
        this.sibus = sibus;
    }

 
}
