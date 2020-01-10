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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.header.Header;

/**
 * The mime version header.
 * MIME-Version = "MIME-Version" HCOLON 1*DIGIT "." 1*DIGIT 
 *
 * @author Assaf Azaria
 */
public interface MimeVersionHeader extends Header
{
    /**
     * Name of MIME-Version Header.
     */
    public final static String name = "MIME-Version";

    //
    // Operations.
    //

    /** 
     * Get the major part of the version.
     */
    public short getMajorVersion();

    /** 
     * Get the minor part of the version.
     */
    public short getMinorVersion();
    
    /** 
     * Set the major version
     * @param major the major version.
     * @throws IllegalArgumentException if the version is not a digit.
     */
    public void setMajorVersion(short major) 
    	throws IllegalArgumentException;

    /** 
     * Set the minor version.
	 * @param minor the minor version.
	 * @throws IllegalArgumentException if the version is not a digit.
	 */
    public void setMinorVersion(short minor) 
    	throws IllegalArgumentException;
    
}
