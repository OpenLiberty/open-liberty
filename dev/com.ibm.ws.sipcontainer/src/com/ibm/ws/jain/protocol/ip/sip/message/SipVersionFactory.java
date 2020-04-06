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
package com.ibm.ws.jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;

/**
 * creates concrete SipVersion instances
 * 
 * @author Moti
 */
public final class SipVersionFactory
{
	/** a default SIP/2.0 version object */
	private static final SipVersion20Impl SIP_VERSION_2_0Impl = new SipVersion20Impl();

	/**
	 * @return a default SIP/2.0 version object
	 */
	public static SipVersion createVersion() {
		return SIP_VERSION_2_0Impl;
	}

	/**
	 * parses input stream into a version object
	 * @param buffer usually contains the string: SIP/2.0
	 * @return version object
	 * @throws SipException when buffer is null or contains unsupported version number
	 */
	public static SipVersion createSipVersion(char[] buffer, int offset, int length)
		throws SipParseException
	{
		if (buffer == null || length < 4) {
			throw new SipParseException("unsupported SIP version");
		}
		if (buffer[offset + 3] == '/' &&
			buffer[offset + 4] == '2' &&
			buffer[offset + 5] == '.' &&
			buffer[offset + 6] == '0')
		{
			return SIP_VERSION_2_0Impl;
		}

		// for all other versions...
		// remember this is an expensive way to create other version.
		// you should better imitate what I did with SipVersionXXImpl
		String otherVersions = new String(buffer, offset, length);
		return new SipVersionImpl(otherVersions);
	}

	/**
	 * creates a version object
	 * @param major SIP major version, such as 2 for 2.0
	 * @param minor SIP minor version, such as 0 for 2.0
	 * @return version object
	 */
	public static SipVersion createSipVersion(int major, int minor) {
		if (major == 2 && minor == 0) {
			return SIP_VERSION_2_0Impl;
		}
		return new SipVersionImpl(major, minor);
	}
}
