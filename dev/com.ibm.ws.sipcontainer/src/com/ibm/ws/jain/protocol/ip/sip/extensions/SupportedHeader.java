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

import jain.protocol.ip.sip.header.OptionTagHeader;

/**
 * For some reason this header is not included in Jain. 
 * 
 * The Supported header field enumerates all the extensions supported by the 
 * UAC or UAS. The Supported header field contains a list of option tags, 
 * described in Section 19.2, that are understood by the UAC or UAS. A UA 
 * compliant to this specification MUST only include option tags corresponding
 * to standards-track RFCs. If empty, it means no extensions are supported. 
 * The compact form of the Supported header field is k. 
 * Example: 
 *
 *    Supported: 100rel
 *
 * @author Assaf Azaria
 */
public interface SupportedHeader extends OptionTagHeader
{
	/**
	 * Name of SupportedHeader
	 */
	public final static String name = "Supported";
}
