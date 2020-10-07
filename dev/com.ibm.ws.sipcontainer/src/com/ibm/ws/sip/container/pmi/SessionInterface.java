/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

/**
 * @author anat, Mar 22, 2005
 */
public interface SessionInterface{
    
//  Constants
    public final static int SIP_SSESSIONS_PER_APP = 11;

    public final static int SIP_APP_SSESSIONS_PER_APP = 12;
        
    /**
     * New SIPSession was created inside the Application
     *  
     */
    public void incrementSipSessionCount();

    /**
     * Remove SipSession from Application
     *  
     */
    public void decrementSipSessionCount();

    /**
     * New Sip Application Session was created in Application
     *  
     */
    public void incrementSipAppSessionCount();

    /**
     * Sip Application session was destroyed
     *  
     */
    public void decrementSipAppSessionCount() ;
    
    /**
     * Update counters that were countered till now
     *
     */
    public void updateCounters();
    
    /**
     * Unregister module 
     */
    public void destroy();
}
