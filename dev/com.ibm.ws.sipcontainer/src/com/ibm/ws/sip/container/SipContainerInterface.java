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
package com.ibm.ws.sip.container;

/**
 * Management interface for the MBean "WebSphere:name=com.ibm.ws.sip.container:type=SipContainerMBean" used as a declarative service.
 * The Liberty profile makes this MBean available in its platform MBean server to allow users to use the dumping utility methods.
 * 
 * @author Galina Rubinshtein, 20 May 2014
 */
public interface SipContainerInterface {

    /**
     * Invokes the dumpAllSASIds operation, which prints a number of
     * all SIP application sessions and the SIP application session IDs.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    public int dumpAllSASIds();

    /**
     * Invokes the dumpAllSASDetails operation, which prints a number of
     * all SIP application sessions and the SIP application session details.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    public int dumpAllSASDetails();

    /**
     * Invokes the dumpAllTUSipSessionIds operation, which prints a number of
     * transaction users and the SIP session IDs within the transaction user (TU), if one exists.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    public int dumpAllTUSipSessionIds();

    /**
     * Invokes the dumpAllTUSipSessionDetails operation, which prints a number of
     * transaction users and details of the SIP session IDs within the transaction user (TU), if one exists.
     * 
     * @return int 0 or -1 as an indication of success or failure
     */
    public int dumpAllTUSipSessionDetails();

    /**
     * Invokes the setDumpMethod operation, which configures an output method for dumping.
     * 
     * @param method String
     *            - use "FILE" if you want the output to go to a specific file
     * @param description String
     *            - specify the file path, including the file name for "FILE" method.
     * 
     * @return String indication of success or failure
     */
    public String setDumpMethod(String method, String description);

    /**
     * Invokes the dumpSASDetails operation, which prints the specific SIP application session details.
     */
    public void dumpSASDetails(String sasId);
    
    /**
     * Invokes the dumpSipSessionDetails operation, which prints the specific SIP session details.
     */
    public void dumpSipSessionDetails(String ssId);
}
