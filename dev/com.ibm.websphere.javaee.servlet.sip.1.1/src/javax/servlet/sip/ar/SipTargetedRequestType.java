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
package javax.servlet.sip.ar;

/**
 * Type of targeted request 
 */
public enum SipTargetedRequestType 
{
	/**
     * The request contains a Request-URI that contains an encoded URI  
     */
    ENCODED_URI, 
    
    /**
	 * The request contains a Join header (RFC 3911) 
	 */
    JOIN, 
    
    /**
     * The request contains a Replaces header (RFC 3891) 
     */
    REPLACES;

}
