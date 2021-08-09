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
 * 
 * Routing regions used in the application selection process.
 *
 */
public enum SipApplicationRoutingRegionType {
	
	/**
	 * The NEUTRAL region contains applications that do not service a specific subscriber.
	 */
	NEUTRAL ,
	
	/**
	 * The ORIGINATING region contains applications that service the caller.
	 */
	ORIGINATING, 
	
	/**
	 * The TERMINATING region contains applications that service the callee.
	 */
	TERMINATING 
}
