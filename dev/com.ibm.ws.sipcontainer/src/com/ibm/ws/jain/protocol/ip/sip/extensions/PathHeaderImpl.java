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

import com.ibm.ws.jain.protocol.ip.sip.header.NameAddressHeaderImpl;

/**
 * Implementation of the Path header
 * 
 * @see com.ibm.ws.jain.protocol.ip.sip.extensions.PathHeader
 * @author ran
 */
public class PathHeaderImpl extends NameAddressHeaderImpl implements PathHeader
{
	/** serialization unique identifier */
	private static final long serialVersionUID = 7385820312069959097L;

	/**
	 * constructor
	 */
	public PathHeaderImpl() {
		super();
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

	/**
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isNested()
	 */
	public boolean isNested() {
		return true;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return super.clone();
	}
}
